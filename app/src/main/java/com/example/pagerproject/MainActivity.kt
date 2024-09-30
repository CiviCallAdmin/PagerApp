package com.example.pagerproject

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.navigation.NavigationView
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import androidx.appcompat.widget.Toolbar
import androidx.drawerlayout.widget.DrawerLayout
import com.example.pagerproject.Profile.Companion
import com.google.firebase.messaging.FirebaseMessaging
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var tabLayout: TabLayout
    private lateinit var toolbar: Toolbar
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var navHeaderDept: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Initialize the toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // Initialize ViewPager2 and TabLayout
        viewPager = findViewById(R.id.viewPager)
        tabLayout = findViewById(R.id.tabLayout)
        drawerLayout = findViewById(R.id.drawer_layout)
        navigationView = findViewById(R.id.nav_view)

        // Set up ViewPager2 with adapter
        viewPager.adapter = ViewPagerAdapter(this)
        val headerView = navigationView.getHeaderView(0)
        val navHeaderFName = headerView.findViewById<TextView>(R.id.nav_header_fName)
        navHeaderDept = headerView.findViewById(R.id.nav_header_dept)
        // Link TabLayout with ViewPager2 using TabLayoutMediator
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Messages"
                1 -> "Compose"
                else -> null
            }
        }.attach()

        // Set up the hamburger icon for navigation drawer
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.burger_menu) // Replace with your menu icon

        // Handle navigation item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_profile -> {
                    // Open ProfileActivity
                    val intent = Intent(this, Profile::class.java)
                    startActivity(intent)
                    overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)
                    true
                }
                else -> false
            }
        }

        // Handle FCM token and save it to the server if it's the first time
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isTokenSaved = sharedPreferences.getBoolean("is_token_saved", false)

        if (!isTokenSaved) {
            // Get FCM token
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val token = task.result
                    Log.d(TAG, token ?: "Token retrieval failed")

                    // Save token to the backend
                    saveTokenToServer(token ?: "")
                } else {
                    Log.w(TAG, "Fetching FCM token failed", task.exception)
                }
            }
        }
        loadUserData(navHeaderFName)
    }
    private fun getDeviceToken(callback: (String) -> Unit) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                // Get the current FCM token
                val token = task.result
                callback(token)
            } else {
                Log.e(Profile.TAG, "Fetching FCM registration token failed", task.exception)
                callback("") // Return an empty string in case of failure
            }
        }
    }
    private fun loadUserData(navHeaderFName: TextView) {
        getDeviceToken { deviceToken ->
            // Check if deviceToken is empty
            if (deviceToken.isEmpty()) {
                Log.e(TAG, "Device token is empty. Unable to load user data.")
                return@getDeviceToken
            }

            // Call the API to fetch existing user data
            val apiService = RetrofitClient.instance
            apiService.getUserData(deviceToken).enqueue(object : Callback<UserDataResponse> {
                override fun onResponse(call: Call<UserDataResponse>, response: Response<UserDataResponse>) {
                    if (response.isSuccessful) {
                        val userData = response.body()
                        if (userData != null && userData.success) {
                            // Populate the fields with existing user data
                            navHeaderFName.text = userData.user_name
                            navHeaderDept.text = userData.department
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
        }
    }

    private fun saveTokenToServer(token: String) {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        val apiService = RetrofitClient.instance

        // Example device information to send
        val deviceInfo = DeviceInfo(
            userName = "",
            profilePic = null,
            deviceToken = token,
            department = ""
        )

        // Send the device information to the server
        apiService.registerDevice(
            userName = deviceInfo.userName,
            profilePic = deviceInfo.profilePic,
            deviceToken = deviceInfo.deviceToken,
            department = deviceInfo.department
        ).enqueue(object : Callback<Void> {
            override fun onResponse(call: Call<Void>, response: Response<Void>) {
                if (response.isSuccessful) {
                    editor.putBoolean("is_token_saved", true)
                    editor.apply()
                    Log.d(TAG, "Token successfully saved to the server.")
                } else {
                    Log.e(TAG, "Failed to save token to the server. Response code: ${response.code()}. Response message: ${response.message()}")
                }
            }

            override fun onFailure(call: Call<Void>, t: Throwable) {
                Log.e(TAG, "Error: ${t.message}")
            }
        })
    }

    // Adapter for the ViewPager2
    private inner class ViewPagerAdapter(activity: AppCompatActivity) : FragmentStateAdapter(activity) {
        override fun getItemCount(): Int = 2

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> FragmentOne()   // Fragment for "Messages" tab
                1 -> FragmentTwo()   // Fragment for "Compose" tab
                else -> FragmentOne()
            }
        }
    }

    // Handle options menu items (e.g. opening the drawer)
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                // Open the navigation drawer
                drawerLayout.open()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, Profile::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.animate_fade_enter, R.anim.animate_fade_exit)
    }
}
