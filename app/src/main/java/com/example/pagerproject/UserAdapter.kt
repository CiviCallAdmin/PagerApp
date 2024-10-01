package com.example.pagerproject

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Filter
import android.widget.Filterable
import com.bumptech.glide.Glide

class UserAdapter(context: Context, private var users: List<UserResponse>) :
    ArrayAdapter<UserResponse>(context, 0, users), Filterable {

    private var filteredUsers: List<UserResponse> = users

    override fun getCount(): Int {
        return filteredUsers.size
    }

    override fun getItem(position: Int): UserResponse? {
        return filteredUsers[position]
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val user = getItem(position)
        val view = convertView ?: LayoutInflater.from(context).inflate(R.layout.dropdown_item, parent, false)

        val profilePic: ImageView = view.findViewById(R.id.profilePic)
        val nameRec: TextView = view.findViewById(R.id.nameRec)
//        val deviceId: TextView = view.findViewById(R.id.deviceId)
        val idNum: TextView = view.findViewById(R.id.idNum)

        nameRec.text = user?.user_name
//        deviceId.text = user?.device_id.toString()
        idNum.text = user?.idNumber

        // Load profile picture using Glide
        Glide.with(context)
            .load("http://192.168.254.163/V4/Others/Kurt/PagerSql/${user?.profile_pic}")
            .placeholder(R.drawable.user) // Placeholder image
            .into(profilePic)

        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val filteredList = if (constraint.isNullOrEmpty()) {
                    users
                } else {
                    val filterPattern = constraint.toString().lowercase().trim()
                    users.filter { user ->
                        user.user_name.lowercase().contains(filterPattern)
                    }
                }
                results.values = filteredList
                return results
            }

            @Suppress("UNCHECKED_CAST")
            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                filteredUsers = results?.values as List<UserResponse>
                notifyDataSetChanged()
            }
        }
    }
}
