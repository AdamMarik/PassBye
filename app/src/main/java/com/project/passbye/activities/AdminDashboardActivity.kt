package com.project.passbye.activities

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.project.passbye.R
import com.project.passbye.database.DatabaseHelper

class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var databaseHelper: DatabaseHelper
    private lateinit var userList: ListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_dashboard)

        databaseHelper = DatabaseHelper(this)
        userList = findViewById(R.id.userList)
        val unlockButton = findViewById<Button>(R.id.unlockButton)
        val deleteButton = findViewById<Button>(R.id.deleteButton)

        loadUserList()

        unlockButton.setOnClickListener {
            val selectedUser = getSelectedUser()
            if (selectedUser != null) {
                if (databaseHelper.unlockUser(selectedUser)) {
                    Toast.makeText(this, "$selectedUser unlocked.", Toast.LENGTH_SHORT).show()
                    loadUserList()
                } else {
                    Toast.makeText(this, "Error unlocking user.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Select a user first.", Toast.LENGTH_SHORT).show()
            }
        }

        deleteButton.setOnClickListener {
            val selectedUser = getSelectedUser()
            if (selectedUser != null) {
                if (databaseHelper.deleteUser(selectedUser)) {
                    Toast.makeText(this, "$selectedUser deleted.", Toast.LENGTH_SHORT).show()
                    loadUserList()
                } else {
                    Toast.makeText(this, "Error deleting user.", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Select a user first.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadUserList() {
        val users = databaseHelper.getAllUsers()
        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_single_choice, users)
        userList.adapter = adapter
        userList.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    private fun getSelectedUser(): String? {
        val position = userList.checkedItemPosition
        return if (position != ListView.INVALID_POSITION) {
            userList.getItemAtPosition(position) as String
        } else {
            null
        }
    }
}
