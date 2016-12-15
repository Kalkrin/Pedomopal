package ca.sheridancollege.pedomopal;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.provider.ContactsContract;

/**
 * Created by Craig on 2016-11-29.
 */

public class DatabaseConnector {
    private static final String DATABASE_NAME = "stats"; //database name
    private SQLiteDatabase database; //database object
    private DatabaseOpenHelper databaseOpenHelper; //database helper
    private static final String COL_1 = "id";
    private static final String COL_2 = "steps";
    private static final String COL_3 = "time";

    private static final String COL_4 = "date";

    public DatabaseConnector(Context context) {
        databaseOpenHelper = new DatabaseOpenHelper(context, DATABASE_NAME, null, 1);
    }

    public void open() {

        database = databaseOpenHelper.getWritableDatabase();
    }

    public boolean insertStat(String steps, String time, String date) {
        ContentValues newStat = new ContentValues();
//        newStat.put("date", date);
//        newStat.put("steps", steps);
//        newStat.put("time", time);

        newStat.put(COL_2,steps);
        newStat.put(COL_3,time);
        newStat.put(COL_4, date);

        open();

        long result = database.insert(DATABASE_NAME, null, newStat);

        if (result == -1)
            return false;
        else
            return true;
//        database.close();
    }

    public Cursor getAllStats() {
        open();
        return database.query("stats", new String[] {"id","steps", "time", "date"},
                null, null, null, null, "id");
    }
}
