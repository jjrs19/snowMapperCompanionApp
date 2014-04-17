package com.logger.app;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.location.Location;
import android.util.Log;

public class LocationDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "trackble.sqlite";
//    private static String DB_PATH = "/data/data/com.gpslog.app/databases/";
    private static final int VERSION = 1;

    SQLiteDatabase db;

    private static final String TABLE_LOCATION = "locations";
    private static final String COLUMN_LOCATION_UUID = "uuid";
    private static final String COLUMN_LOCATION_TIMESTAMP = "timestamp";
    private static final String COLUMN_LOCATION_LATITUDE = "latitude";
    private static final String COLUMN_LOCATION_LONGITUDE = "longitude";
    private static final String COLUMN_LOCATION_ALTITUDE = "altitude";
    private static final String COLUMN_LOCATION_SPEED = "speed";
    private static final String COLUMN_LOCATION_HEADING = "heading";
    private static final String COLUMN_LOCATION_ACCURACY = "accuracy";
    private static final String COLUMN_LOCATION_PROVIDER = "provider";
    private static final String TAG = "LocationDatabaseHelper";

//    private static String uuid;

    public LocationDatabaseHelper(Context context){
        super(context, DB_NAME, null, VERSION);
        Log.d(TAG, "database helper created");

        db = getWritableDatabase();
        onCreate(db);

//        GpsTrackerActivity gpsApp = (GpsTrackerActivity)context.getApplicationContext();

    }

    @Override
    public void onCreate(SQLiteDatabase db) {
//        db = getWritableDatabase();
        Log.d(TAG, "locations table created");
        db.beginTransaction();
        try {
            // Create the "location" table
            db.execSQL("CREATE TABLE locations (" +
                    " uuid varchar(100)," +
                    " timestamp integer, " +
                    " latitude real, " +
                    " longitude real, " +
                    " altitude real, " +
                    " speed real, " +
                    " heading real, " +
                    " accuracy real, " +
                    " provider varchar(100)" +
                    " )");
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Implement schema changes and data massage here when upgrading
    }

    public long insertLocation(Location location) {
        Log.d(TAG, "inserted loc:" + location.getLatitude() + " lon:" + location.getLongitude());

        ContentValues cv = new ContentValues();

        cv.put(COLUMN_LOCATION_UUID, "test");
        cv.put(COLUMN_LOCATION_TIMESTAMP, location.getTime());
        cv.put(COLUMN_LOCATION_LATITUDE, location.getLatitude());
        cv.put(COLUMN_LOCATION_LONGITUDE, location.getLongitude());
        cv.put(COLUMN_LOCATION_ALTITUDE, location.getAltitude());
        cv.put(COLUMN_LOCATION_SPEED, location.getSpeed());
        cv.put(COLUMN_LOCATION_HEADING, location.getBearing());
        cv.put(COLUMN_LOCATION_ACCURACY, location.getAccuracy());
        cv.put(COLUMN_LOCATION_PROVIDER, location.getProvider());

        return db.insert(TABLE_LOCATION, null, cv);
    }
}