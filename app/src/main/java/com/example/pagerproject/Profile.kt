package com.example.pagerproject

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.example.pagerproject.databinding.ActivityProfileBinding
import com.google.firebase.messaging.FirebaseMessaging
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.io.File
import java.io.IOException

class Profile : AppCompatActivity() {
    private lateinit var binding: ActivityProfileBinding
    private val REQUEST_CAMERA_PERMISSION = 2
    private var capturedImageUri: Uri? = null
    private var selectedImageUri: Uri? = null
    private val FILE_PROVIDER_AUTHORITY = "com.example.pagerproject.fileprovider"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        enableEdgeToEdge()

        ViewCompat.setOnApplyWindowInsetsListener(binding.profile) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Load existing user data when the activity is created
        loadUserData()

        // Handle the save button click
        binding.savebtn.setOnClickListener {
            val userName = binding.fullName.text.toString()
            val idNum = binding.idNum.text.toString()

            // Retrieve the current device token
            getDeviceToken { deviceToken ->
                // Validate user input
                if (userName.isNotBlank() && idNum.isNotBlank() && deviceToken.isNotBlank()) {
                    // Call the PHP script to save/update user data
                    saveUserData(deviceToken, userName, idNum)
                } else {
                    Toast.makeText(
                        this,
                        "Please fill in all fields and ensure the token is valid",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // Back button click listener
        binding.backbtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)

        }
        binding.profileImage.setOnClickListener {
            checkAndRequestPermissions()
        }
    }
    private fun loadUserData() {
        getDeviceToken { deviceToken ->
            if (deviceToken.isNotBlank()) {
                // Call the API to fetch existing user data
                val apiService = RetrofitClient.instance
                apiService.getUserData(deviceToken).enqueue(object : Callback<UserDataResponse> {
                    override fun onResponse(
                        call: Call<UserDataResponse>,
                        response: Response<UserDataResponse>
                    ) {
                        if (response.isSuccessful) {
                            val userData = response.body()
                            if (userData != null && userData.success) {
                                // Populate the fields with existing user data
                                binding.fullName.setText(userData.user_name)
                                binding.userName.setText(userData.user_name)
                                binding.idNum.setText(userData.idNumber)

                                // Check if profile_pic is not null before constructing the URL
                                val profileImagePath = userData.profile_pic // Should be something like "profilePic/profile_1727670832224.jpg"
                                if (profileImagePath != null) {
                                    val baseUrl = "http://192.168.254.163/V4/Others/Kurt/PagerSql/"
                                    val profileImageUrl = "${baseUrl}${profileImagePath}" // Complete URL
                                    // Load the profile picture
                                    loadProfileImage(profileImageUrl)
                                } else {
                                    Log.e(TAG, "Profile image path is null.")
                                }
                            } else {
                                Log.e(TAG, userData?.message ?: "No user data found.")
                            }
                        } else {
                            Log.e(TAG, "Failed to load user data. Response code: ${response.code()}")
                        }
                    }

                    override fun onFailure(call: Call<UserDataResponse>, t: Throwable) {
                        Log.e(TAG, "Error: ${t.message}")
                    }
                })
            } else {
                Log.e(TAG, "FCM token is empty")
            }
        }
    }



    private fun loadProfileImage(url: String) {
        Log.d(TAG, "Loading profile image from URL: $url") // Log the URL being used
        Glide.with(this)
            .load(url)
            .placeholder(R.drawable.profilenone) // Use your placeholder drawable
            .error(R.drawable.profilenone) // Use your error drawable
            .into(binding.profileImage) // Bind to your ImageView
    }

    private fun saveUserData(deviceToken: String, userName: String, idNum: String) {
        val apiService = RetrofitClient.instance

        // Convert strings to RequestBody
        val tokenRequestBody = deviceToken.toRequestBody("text/plain".toMediaTypeOrNull())
        val nameRequestBody = userName.toRequestBody("text/plain".toMediaTypeOrNull())
        val idNumRequestBody = idNum.toRequestBody("text/plain".toMediaTypeOrNull())

        // Create the image file part from either capturedImageUri or selectedImageUri
        val imageUri = capturedImageUri ?: selectedImageUri // Use capturedImageUri if available, otherwise use selectedImageUri

        val profilePicPart = imageUri?.let { uri ->
            val inputStream = contentResolver.openInputStream(uri)
            val fileBytes = inputStream?.readBytes() ?: byteArrayOf() // Read the file as bytes

            // Generate a unique file name based on timestamp
            val timestamp = System.currentTimeMillis()
            val uniqueFileName = "profile_$timestamp.jpg"

            // Create the file part with the unique file name
            val requestFile = fileBytes.toRequestBody("image/jpeg".toMediaTypeOrNull())
            MultipartBody.Part.createFormData("profile_pic", uniqueFileName, requestFile)
        }

        // Show loading dialog
        val loadingDialog = createLoadingDialog()
        loadingDialog.show()

        // Perform the API call
        apiService.saveProfile(
            tokenRequestBody,
            nameRequestBody,
            idNumRequestBody,
            profilePicPart
        ).enqueue(object : Callback<ApiResponse> {
            override fun onResponse(call: Call<ApiResponse>, response: Response<ApiResponse>) {
                // Dismiss the loading dialog
                loadingDialog.dismiss()

                if (response.isSuccessful) {
                    val apiResponse = response.body()
                    if (apiResponse != null) {
                        showSuccessDialog(apiResponse.message) // Show success dialog
                    }
                } else {
                    Log.e(TAG, "Failed to save profile. Response code: ${response.code()}")
                    Toast.makeText(
                        this@Profile,
                        "Failed to save profile. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<ApiResponse>, t: Throwable) {
                // Dismiss the loading dialog
                loadingDialog.dismiss()

                Log.e(TAG, "Error: ${t.message}")
                Toast.makeText(this@Profile, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    // Create a loading dialog
    private fun createLoadingDialog(): AlertDialog {
        val builder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView = inflater.inflate(R.layout.loading_layout, null) // Create a layout for the loading dialog
        builder.setView(dialogView)
        builder.setCancelable(false) // Prevent user from dismissing the dialog

        return builder.create()
    }

    // Show success dialog
    private fun showSuccessDialog(message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Success")
        builder.setMessage(message)
        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }
        val dialog = builder.create()
        dialog.show()
    }


    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the current FCM token
                val token = task.result
                callback(token)
            } else {
                Log.e(TAG, "Fetching FCM registration token failed", task.exception)
                callback("") // Return an empty string in case of failure
            }
        }
    }

    private fun checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.CAMERA
            ) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_CAMERA_PERMISSION
            )
        } else {
            // Permission already granted, proceed with taking a picture
            showImageDialog()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Camera permission granted, proceed with taking a picture
                    showImageDialog()
                } else {
                    // Camera permission denied, handle accordingly
                    Toast.makeText(
                        this,
                        "Camera Permission denied. Go to your Phone Setting to Allow it.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

        }
    }

    private fun dismissCustomDialog() {
        if (isImageDialogShowing) {

            isImageDialogShowing = false
        }
    }

    private var isImageDialogShowing = false
    private fun showImageDialog() {
        // Check if the dialog is already showing, and if so, return early
        if (isImageDialogShowing) {
            return
        }
        dismissCustomDialog()
        val dialogView = layoutInflater.inflate(R.layout.profileedit_popup, null)
        val lytCameraPick = dialogView.findViewById<LinearLayout>(R.id.lytCameraPick)
        val lytGalleryPick = dialogView.findViewById<LinearLayout>(R.id.lytGalleryPick)

        val alertDialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()
        alertDialog.window?.attributes?.windowAnimations = R.style.DialogAnimationShrink

        // Set the background to be transparent
        alertDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        lytCameraPick.setOnClickListener {
            takePicture()
            alertDialog.dismiss() // Close the dialog after clicking "Take a photo"
            // Reset the flag when dismissing the dialog
            isImageDialogShowing = false
        }

        lytGalleryPick.setOnClickListener {
            chooseFromGallery()
            alertDialog.dismiss() // Close the dialog after clicking "Choose from gallery"
            // Reset the flag when dismissing the dialog
            isImageDialogShowing = false
        }

        alertDialog.setOnDismissListener {
            // Reset the flag when dismissing the dialog
            isImageDialogShowing = false
        }

        alertDialog.show()
        // Set the flag to true when the dialog is displayed
        isImageDialogShowing = true
    }

    private fun chooseFromGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        selectImageLauncher.launch(intent)
    }

    private val selectImageLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                selectedImageUri = result.data?.data
                binding.profileImage.setImageURI(selectedImageUri)
            }
        }
    private val REQUEST_IMAGE_CAPTURE = 2
    private fun takePicture() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        // Ensure that there is a camera activity to handle the intent
        if (cameraIntent.resolveActivity(packageManager) != null) {
            // Create the File where the photo should go
            val photoFile: File? = try {
                createImageFile()
            } catch (ex: IOException) {
                // Error occurred while creating the File
                Log.e("UploadVerificationFile", "Error creating image file", ex)
                null
            }

            // Continue only if the File was successfully created
            photoFile?.let {
                val photoURI: Uri = FileProvider.getUriForFile(
                    this,
                    FILE_PROVIDER_AUTHORITY,
                    it
                )
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI)

                // Save the captured image URI to the class variable
                capturedImageUri = photoURI

                // Start the image capture intent
                startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE)
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        // Create an image file name
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${System.currentTimeMillis()}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            capturedImageUri?.let {

                binding.profileImage.setImageURI(capturedImageUri)
            }
        }
    }

    companion object {
        const val TAG = "Profile"
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)
    }
}
