package com.example.food_trock.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Window
import android.view.WindowManager
import android.widget.*
import androidx.cardview.widget.CardView
import com.bumptech.glide.Glide
import com.example.food_trock.R
import com.example.food_trock.fragments.MenuListFragment
import com.example.food_trock.models.Store
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import java.util.*




private lateinit var bottomNavigationView: BottomNavigationView

class OwnerSettingsActivity : AppCompatActivity() {

    lateinit var db: FirebaseFirestore
    lateinit var auth: FirebaseAuth
    lateinit var editTruckName: EditText
    lateinit var editFullName: EditText
    lateinit var ownerProfileIMG: ImageView
    lateinit var txtStatus: TextView
    lateinit var txtEmail: TextView
    lateinit var cardViewMenu: CardView
    val foodTruckCollectionRef = Firebase.firestore.collection("FoodTrucks")
    var selectedPhotoUri: Uri? = null


    private val navigation = BottomNavigationView.OnNavigationItemSelectedListener { item ->
        when (item.itemId) {
            R.id.favourites -> {
                bottomNavigationView.menu.getItem(3).isChecked = false
                val intent = Intent(this, FavouritesActivity::class.java)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
            R.id.trucks -> {
                bottomNavigationView.menu.getItem(3).isChecked = false
                val intent = Intent(this, StoreActivity::class.java)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true
            }
            R.id.settings -> {
                item.isChecked = true
                return@OnNavigationItemSelectedListener false
            }
            R.id.maps -> {
/*                val intent = Intent(this@MainActivity, MyRecipes::class.java)
                startActivity(intent)
                return@OnNavigationItemSelectedListener true*/
            }
        }
        false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.requestWindowFeature(Window.FEATURE_NO_TITLE); this.getWindow().setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
        );
        setContentView(R.layout.activity_owner_settings)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnNavigationItemSelectedListener(navigation)
        bottomNavigationView.menu.getItem(3).isChecked = true

        var saveTruckNameBtn = findViewById<Button>(R.id.actionBtn)
        var saveFullNameBtn = findViewById<Button>(R.id.saveFullNameBtn)
        var logOutBTN = findViewById<Button>(R.id.bt_Logout)
        var switchBtn = findViewById<Switch>(R.id.switchBtn)
        txtStatus = findViewById(R.id.txtStatus)
        db = Firebase.firestore
        auth = Firebase.auth

        editTruckName = findViewById(R.id.editTruckName)
        editFullName = findViewById(R.id.editFullName)
        txtEmail = findViewById(R.id.txtEmail)
        ownerProfileIMG = findViewById(R.id.ownerProfileImage)
        cardViewMenu = findViewById(R.id.cardViewMenus)


        /** Checks if the owners shop is online or offline.
         * Presets the switch status as well as profile picture
         */

        auth.currentUser?.let {
            foodTruckCollectionRef.document(it.uid).get().addOnSuccessListener { result ->
                if (result != null) {
                    val store = result.toObject(Store::class.java)
                    if (store != null) {
                        if (store.storeStatus) {
                            switchBtn.isChecked = true
                        }
                        Glide.with(this)
                            .load(store.storeImage)
                            .into(ownerProfileIMG)
                        editTruckName.setText(store.storeName)
                        editFullName.setText(store.fullName)
                        txtEmail.text = auth.currentUser!!.email

                    }
                }
            }
        }


        /** Calls uploadImageToFirebaseStorage function which in turn calls getNewStoreMap.
         */
        saveTruckNameBtn.setOnClickListener() {
            getNewStoreMap()
        }
        saveFullNameBtn.setOnClickListener() {
            getNewStoreMap()
        }
        cardViewMenu.setOnClickListener() {
            val menuFragment = MenuListFragment()

            val transaction = supportFragmentManager.beginTransaction()
            transaction.add(R.id.menuContainer, menuFragment, "menu")
            transaction.commit()
        }



        /** Checks and unchecks the switch button. If checked, uncheck and display offline vice versa.
         */
        switchBtn.setOnCheckedChangeListener { _, isChecked ->
            when (switchBtn.isChecked) {
                true -> {
                    txtStatus.text = "ONLINE"
                    txtStatus.setTextColor(Color.parseColor("#FF5EC538"))
                    auth.currentUser?.let {
                        foodTruckCollectionRef.document(it.uid).update("storeStatus", true)
                    }
                }
                false -> {
                    txtStatus.text = "OFFLINE"
                    txtStatus.setTextColor(Color.parseColor("#837E7E"))
                    auth.currentUser?.let {
                        foodTruckCollectionRef.document(it.uid).update("storeStatus", false)
                    }
                }
            }


        }


        /**
         * Starts the intent to pick an image from gallery.
         */
        ownerProfileIMG.setOnClickListener() {
            Log.e("TEST", "photo clicked")

            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 0)
        }

        logOutBTN.setOnClickListener {
            logOut()
        }
    }

    /**
     * Turns the selected photo into a bitmap and sets the image.
     */

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)


        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            Log.e("TEST","onActivityResult: photo was selected")

            selectedPhotoUri = data.data

            val bitmap = MediaStore.Images.Media.getBitmap(contentResolver,selectedPhotoUri)

            ownerProfileIMG.setImageBitmap(bitmap)



            uploadImageToFirebaseStorage()
        }
    }

    /**
     * Creates a new store map based on info from editTexts and takes in a profileImageURL
     * sets the map into database and merges it leaving unchanged values as they were.
     */
    private fun getNewStoreMap (profileImageURL: String = "") {
        val truckName = editTruckName.text.toString()
        val fullName = editFullName.text.toString()
        val truckImage = profileImageURL
        val map = mutableMapOf<String, Any>()
        if (truckName.isNotEmpty()) {
            map["storeName"] = truckName
        }
        if (truckImage.isNotEmpty()) {
            map["storeImage"] = truckImage
        }
        if (fullName.isNotEmpty()) {
            map["fullName"] = fullName
        }
        auth.currentUser?.let {
            foodTruckCollectionRef.document(it.uid).set(map, SetOptions.merge())
        }
    }


    private fun requestLocationPermission () {
        // TODO: 2021-10-18 Ask for location permission and set the current location of truck
        // TODO: Function is placed inside switch status button.
    }


    /**
     * Generates a filename with random UUID and loads it into /images.
     * selectedPhotoUri gets placed under the filename and the url is downloaded and sent to
     * getNewStoreMap() for mapping.
     */
    private fun uploadImageToFirebaseStorage() {

        if(selectedPhotoUri != null) {
            //val filename = UUID.randomUUID().toString()
            val filename = auth.currentUser?.uid
            val imagesRef = FirebaseStorage.getInstance().getReference("/images/$filename")

            imagesRef.putFile(selectedPhotoUri!!)
                .addOnSuccessListener {
                    Log.e("TEST", "onUpload: successfully uploaded image: ${it.metadata?.path}")

                    imagesRef.downloadUrl.addOnSuccessListener {
                        it.toString()
                        Log.e("TEST", "onUpload: FileLocation: $it")

                        getNewStoreMap(it.toString())

                    }
                }
        }
    }

    /**
     * Logs the truck owner out of the app. Returns to login activity.
     */
    fun logOut(){
        if(auth.currentUser != null){
            auth.signOut()
            val intent= Intent(this, LoginActivity::class.java )
            startActivity(intent)
        }

    }

}