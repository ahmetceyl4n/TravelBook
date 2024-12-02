package com.ahmetceylan.travelbook.roomdb

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.ahmetceylan.travelbook.model.Place
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Flowable

@Dao
interface PlaceDao {

    @Query("Select * from Place ")
    fun getAll() : Flowable<List<Place>>

    @Insert
    fun insert(place:Place) : Completable

    @Delete
    fun delete(place: Place) : Completable

}