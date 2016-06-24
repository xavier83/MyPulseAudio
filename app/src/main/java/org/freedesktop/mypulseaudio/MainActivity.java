package org.freedesktop.mypulseaudio;

import android.content.res.AssetManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import org.apache.commons.io.FileSystemUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });


        FloatingActionButton launch = (FloatingActionButton) findViewById(R.id.launch);
        launch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Starting Pulseaudio Server", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
                startPulseaudioServer();
            }
        });

        copyPulseBinaries();

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
    }

    void copyPulseBinaries(){
        Log.v("getFilesDir()",getFilesDir().getPath());
        final AssetManager am = getAssets();
        final String destpath = combine(getFilesDir().getPath(),"sysroot");
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                if(!(new File(destpath)).exists()) {
                    copyAssetFolder(am, "sysroot", destpath);
                }
            }
        });
    }

    void startPulseaudioServer(){
        File pabin = new File(getFilesDir(),"sysroot/usr/bin/pulseaudio");
        File palib1 = new File(getFilesDir(),"sysroot/usr/lib/pulseaudio");
        File palib2 = new File(getFilesDir(),"sysroot/usr/lib64/");
        File palib3 = new File(getFilesDir(),"sysroot/usr/lib/");
        TextView outputview = (TextView) findViewById(R.id.outputview);
        if(pabin.exists()) {
            if (pabin.canExecute())
                pabin.setExecutable(true);
            Process process = null;
            outputview.setText("Starting pulseaudio daemon...\n\n");
            try {
                String[] envp = {"LD_LIBRARY_PATH="+palib1.getAbsolutePath()+":"+palib2.getAbsolutePath()+":"+palib3.getAbsolutePath()+":$LD_LIBRARY_PATH"};
                Log.v("pulseaudio environment",envp[0]);
                process = Runtime.getRuntime().exec(pabin.getAbsolutePath(),envp);
//            DataOutputStream os = new DataOutputStream(process.getOutputStream());
                DataInputStream osRes = new DataInputStream(process.getInputStream());
                streamToView(osRes, outputview);
            } catch (IOException e) {
                Log.v("Execution Failed", "pulseaudio launch error");
                outputview.append("Error: pulseaudio launch failed...\n\n");
                e.printStackTrace();
            }
        }
        else{
            Log.v("pulseaudio","binary not found");
        }
    }

    void streamToView(final DataInputStream out, final TextView view){
        Thread svThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Log.v("pulseaudio","thread started");
                try {
                    byte [] buffer = new byte[1000];
                    int status = out.read(buffer);
                    Log.v("pulseaudio","buffer read "+status);
                    while (status!=-1){
                        final String outputText = new String(buffer);
                        Log.v("pulseaudio",outputText);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                view.append(outputText);
                            }
                        });
                    }
                } catch (Exception e){
                    e.printStackTrace();
                    Log.v("pulseaudio","buffer read error");
                }
            }
        });
        svThread.start();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    public static String combine(String path1, String path2)
    {
        File file1 = new File(path1);
        File file2 = new File(file1, path2);
        return file2.getPath();
    }

    private static boolean copyAssetFolder(AssetManager assetManager,
                                           String fromAssetPath, String toPath) {
        try {
            String[] files = assetManager.list(fromAssetPath);
            new File(toPath).mkdirs();
            boolean res = true;
            for (String file : files) {
                String fromFile =  (new File(fromAssetPath,file)).getPath();
                String toFile = toPath + "/" + file;
                String [] contents = assetManager.list(fromFile);
                if(contents!=null && contents.length > 0) {
                    Log.v("Copying folder ... ", fromFile);
                    res &= copyAssetFolder(assetManager,
                            fromAssetPath + "/" + file,
                            toPath + "/" + file);
                }else {

//                    Log.v("pulseaudio", "Copying file ... "+fromFile+" to "+toFile);
                    res &= copyAsset(assetManager, fromFile, toFile);
                    if(toFile.contains("bin")){
                        new File(toFile).setExecutable(true);
//                        Log.v("permission", "Making executable "+toPath);
                    }
                }
            }
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static boolean copyAsset(AssetManager assetManager,
                                     String fromAssetPath, String toPath) {
        InputStream in = null;
        OutputStream out = null;
        try {
            in = assetManager.open(fromAssetPath);
            new File(toPath).createNewFile();
            out = new FileOutputStream(toPath);
            copyFile(in, out);
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
            return true;
        } catch(Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static void copyFile(InputStream in, OutputStream out) throws IOException {
        byte[] buffer = new byte[1024];
        int read;
        while((read = in.read(buffer)) != -1){
            out.write(buffer, 0, read);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            // Handle the camera action
        } else if (id == R.id.nav_gallery) {

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        } else if (id == R.id.nav_share) {

        } else if (id == R.id.nav_send) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
