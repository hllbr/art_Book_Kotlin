package com.hllbr.artbookkotlin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.ByteArrayOutputStream


class MainActivity2 : AppCompatActivity() {
    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)
        val intent = intent
        val info = intent.getStringExtra("info")
        if(info.equals("new")){
            artNameText.setText("")
            artistNameText.setText("")
            dateText.setText("")
            button.visibility = View.VISIBLE
            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.addimage)
            imageView2.setImageBitmap(selectedImageBackground)
        }else{
           button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)
            try{
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                val cursor = database.rawQuery("SELECT * FROM arts WHERE id = ? ", arrayOf(selectedId.toString()))
                val nameIx = cursor.getColumnIndex("artname")
                val artistNameIx = cursor.getColumnIndex("artistname")
                val yearIx = cursor.getColumnIndex("year")
                val idIx = cursor.getColumnIndex("id")
                val imageIx = cursor.getColumnIndex("image")
                while (cursor.moveToNext()){
                    artNameText.setText(cursor.getString(nameIx))
                    artistNameText.setText(cursor.getString(artistNameIx))
                    dateText.setText(cursor.getString(yearIx))
                val byteArray = cursor.getBlob(imageIx)
                    val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                    imageView2.setImageBitmap(bitmap)
                }
              //  arrayAdapter.notifyDataSetChanged()
                cursor.close()
            }catch (e : Exception){
                println("Main try-catch : "+e.localizedMessage.toString())
            }
        }
    }
    fun selectImage(view : View){
     if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
         ActivityCompat.requestPermissions(this,arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
     }else{
         val intentToGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
         startActivityForResult(intentToGallery,2)
     }

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        //istek kodum 1 /2 değil 2 izin için değil direkt galeriye gitmek için yazılan yapı
        if(requestCode == 1){
            if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                val ıntentToGalley = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                startActivityForResult(ıntentToGalley,2)
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if(requestCode == 2){
            if (resultCode == RESULT_OK){
                if (data != null){
                    try{


                    selectedPicture = data.data

                    if(selectedPicture != null) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            val source =
                                ImageDecoder.createSource(this.contentResolver, selectedPicture!!)
                            selectedBitmap = ImageDecoder.decodeBitmap(source)
                            imageView2.setImageBitmap(selectedBitmap)
                        } else {
                            selectedBitmap= MediaStore.Images.Media.getBitmap(
                                this.contentResolver,
                                selectedPicture
                            )
                            imageView2.setImageBitmap(selectedBitmap)
                        }
                    }
                    }catch (e : Exception){
                       // e.printStackTrace()
                        println("TRY & CATCH = "+e.localizedMessage)
                    }

                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data)
    }
    fun save(view : View){
        val artName = artNameText.text.toString()
        val artistName = artistNameText.text.toString()
        val year = dateText.text.toString()
        //Resimleri veri tabanlarına kaydederken resim olarak değil veri olarak kaydederiz

        if(selectedBitmap != null){
            val smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)


            var outputStream = ByteArrayOutputStream()
            // selectedBitmap?.compress(Bitmap.CompressFormat.PNG,50,outputStream)//Resmi sıkıştırıp bir veriye dönüştürmek için kullanılan bir method
            smallBitmap.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            var byteArray = outputStream.toByteArray()
            //SQLite içerisinde resim boyutları çok önemli
            try{
                val database = this.openOrCreateDatabase("Arts", MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts(id INTEGER PRIMARY KEY,artname VARCHAR,artistname VARCHAR,year VARCHAR,image BLOB)")
                //BLOB SQLite içinde veri kaydetmek için kullsnılsn bir veri türüdür.
                val sqlString = "INSERT INTO arts(artname,artistname,year,image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()
            }catch (e : Exception){
                println("try-catch = "+e.localizedMessage.toString())
            }
      // finish()
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)

        }


    }
    fun makeSmallerBitmap(image : Bitmap,maximumSize : Int) : Bitmap{
        var width = image.width
        var height = image.height

        var bitmapRatio : Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1 ){
            width = maximumSize
            val scaledHeight = width /bitmapRatio
          //  height = width / bitmapRatio.toInt()
            height = scaledHeight.toInt()

        }else{
            height = maximumSize
            val scaledWidth = height * bitmapRatio
          //  width = height * bitmapRatio.toInt()
            width = scaledWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }
}