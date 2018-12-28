package jmapps.strengthofwill.DBSetup;

import android.content.Context;

import com.readystatesoftware.sqliteasset.SQLiteAssetHelper;

public class DBAssetHelper extends SQLiteAssetHelper {

    private static final String DBName = "StrenghtOfWillDB";
    private static final int DBVersion = 5;

    public DBAssetHelper(Context context) {
        super(context, DBName, null, DBVersion);

        setForcedUpgrade(DBVersion);
    }
}