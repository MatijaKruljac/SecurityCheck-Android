package com.securitycheck.demoapp

import android.content.Context
import android.os.Bundle
import android.os.Environment
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.securitycheck.demoapp.databinding.FragmentFirstBinding
import com.securitycheck.demoapp.services.database.DBCipherHandler
import com.securitycheck.demoapp.services.database.DBHandler
import com.securitycheck.demoapp.services.datastore.DataStoreHandler
import com.securitycheck.demoapp.services.helpers.CryptoManager
import com.securitycheck.demoapp.services.helpers.User
import com.securitycheck.demoapp.services.keystore.KeyStoreManager
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private object PreferenceKeys {
        val USER = stringPreferencesKey("user")
    }

    private var dbHandler: DBHandler? = null
    private var dbCipherHandler: DBCipherHandler? = null
    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dbHandler = DBHandler(requireContext())
        dbCipherHandler = DBCipherHandler(requireContext(), "security-check-passphrase")

        binding.buttonFirst.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onStart() {
        super.onStart()
        testSQLiteDataEncryption()
        testSQLiteCipherDateEncryption()
        testDataStoreEncryption()
        testKeyStoreEncryption()
        testFileDataEncryption()
    }

    private fun testSQLiteDataEncryption() {
        dbHandler?.deleteAllUsers()

        val cryptoManager = CryptoManager(requireContext(), "SCDBKey")

        // Encrypt
        val firstName = "Ivo"
        val encryptedFirstName = cryptoManager.encrypt(firstName)
        val encodedFirstNameString: String = Base64.encodeToString(encryptedFirstName, Base64.DEFAULT)

        val lastName = "Ivić"
        val encryptedLastName = cryptoManager.encrypt(lastName)
        val encodedLastNameString: String = Base64.encodeToString(encryptedLastName, Base64.DEFAULT)

        dbHandler?.addUser(encodedFirstNameString, encodedLastNameString)

        // Decrypt
        val user = dbHandler?.getAllUsers()!!.first()

        val savedFirstName = user.firstName
        val decodedFirstNameByteArray: ByteArray = Base64.decode(savedFirstName, Base64.DEFAULT)
        val decryptedFirstName = cryptoManager.decrypt(decodedFirstNameByteArray)
        print(decryptedFirstName)

        val savedLastName = user.lastName
        val decodedLastNameByteArray: ByteArray = Base64.decode(savedLastName, Base64.DEFAULT)
        val decryptedLastName = cryptoManager.decrypt(decodedLastNameByteArray)
        print(decryptedLastName)
    }

    private fun testSQLiteCipherDateEncryption() {
        dbCipherHandler?.deleteAllUsers()
        dbCipherHandler?.addUser("Matija", "Kruljac")

        val userList = dbCipherHandler?.getAllUsers()
        print(userList)
    }

    private fun testDataStoreEncryption() {
        GlobalScope.launch {
            val user = User(7,"James", "Bond")
            val userToString = user.toString()
            val cryptoManager = CryptoManager(requireContext(), "SCKey")
            val dataStoreHandler = DataStoreHandler(requireContext())

            // Encrypt
            val encryptedUser = cryptoManager.encrypt(userToString)
            val encodedString: String = Base64.encodeToString(encryptedUser, Base64.DEFAULT)
            dataStoreHandler.saveStringValue(encodedString, PreferenceKeys.USER)

            dataStoreHandler
                .getStringValueFlow(PreferenceKeys.USER)
                .onEach { user ->
                    // Decrypt
                    val decodedByteArray: ByteArray = Base64.decode(user, Base64.DEFAULT)
                    val decryptedUser = cryptoManager.decrypt(decodedByteArray)
                    print(decryptedUser)
                }
                .collect()
            }
    }

    private fun testKeyStoreEncryption() {
        val keyStoreManager = KeyStoreManager(requireContext())

        val password = "my_password_2604"
        val key = "password_key"

        // Save the password
        keyStoreManager.saveString(password, key)

        // Retrieve the password
        val retrievedPassword = keyStoreManager.getString(key)

        if (retrievedPassword != null) {
            println(retrievedPassword)
        } else {
            println("Password retrieval failed")
        }
    }

    private fun testFileDataEncryption() {
        val context: Context = requireContext()
        val cryptoManager = CryptoManager(requireContext(), "SCFileKey")
        val data = "Some secret data"

        // Encrypt
        val encryptedData = cryptoManager.encrypt(data)
        val encodedEncryptedData: String = Base64.encodeToString(encryptedData, Base64.DEFAULT)

        // Internal storage
        val iFileName = "Internal_File.txt"
        val iFos: FileOutputStream = context.openFileOutput(iFileName, Context.MODE_PRIVATE)
        iFos.write(encodedEncryptedData.toByteArray())
        iFos.close()

        // External storage
        try {
            val eFileName = "External_File.txt"
            val externalStorageDir = Environment.getExternalStorageDirectory()
            val file = File(externalStorageDir, eFileName)
            val eFos = FileOutputStream(file)
            eFos.write(encodedEncryptedData.toByteArray())
            eFos.close()
        } catch (e: Exception) {
            println(e.message)
        }
    }
}