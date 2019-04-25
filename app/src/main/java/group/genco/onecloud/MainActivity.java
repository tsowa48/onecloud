package group.genco.onecloud;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.design.widget.NavigationView;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.ArraySet;
import android.view.ContextMenu;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.Spinner;
import android.widget.TextView;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import group.genco.onecloud.cloud.*;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    Cloud cloud;
    List<File> files;
    ListView listView;
    FileAdapter adapter;
    List<String> fullPath;

    NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton btnUpload = findViewById(R.id.fab);
        btnUpload.setOnClickListener(view -> Snackbar.make(view, "Действие недоступно", Snackbar.LENGTH_LONG).setAction("OneCloud", null).show());

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        navigationView = findViewById(R.id.nav_view);
        navigationView.setItemIconTintList(null);
        navigationView.setNavigationItemSelectedListener(this);

        listView = findViewById(R.id.listView);


        listView.setClickable(true);
        listView.setOnItemClickListener((arg0, arg1, position, arg3) -> {
            String name = listView.getItemAtPosition(position).toString();
            if("..".equals(name)) {
                fullPath.remove(fullPath.size() - 1);
            } else {
                fullPath.add(name);
            }
            LoadPath();
        });
        registerForContextMenu(listView);


        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 0);
    }

    @Override
    protected void onResume() {
        super.onResume();
        fullPath = new ArrayList<>();
        cloud = new Cloud();
        fullPath.add("/");
        LoadPrefs();
        LoadPath();
        LoadMenu();
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_add_cloud) { // Add new Cloud
            LinearLayout popupView = new LinearLayout(this);
            popupView.setBackgroundTintList(null);
            popupView.setOrientation(LinearLayout.VERTICAL);

            Spinner driveNamesList = new Spinner(this);
            String[] data = new String[] {"Яндекс.Диск", "Dropbox"};// <---------------- TODO: add new drives HERE
            ArrayAdapter drives = new ArrayAdapter(this,android.R.layout.simple_list_item_1, data);
            driveNamesList.setAdapter(drives);

            //TextView tv = new TextView(this);
            //tv.setText("Токен:");
            EditText txtToken = new EditText(this);
            ClipboardManager clipboardManager=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            txtToken.setOnLongClickListener(v-> {
                ClipData _data = clipboardManager.getPrimaryClip();
                ClipData.Item _item = _data.getItemAt(0);

                String text = _item.getText().toString();
                txtToken.setText(text);
                return true;
            });

            Button btnAddCloud = new Button(this);
            btnAddCloud.setText("Добавить");

            popupView.addView(driveNamesList);
            //popupView.addView(tv);
            popupView.addView(txtToken);
            popupView.addView(btnAddCloud);

            txtToken.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(txtToken, InputMethodManager.SHOW_IMPLICIT);

            PopupWindow popUp = new PopupWindow(this);

            btnAddCloud.setOnClickListener(v-> {
                String token = txtToken.getText().toString();
                String driveName = driveNamesList.getSelectedItem().toString();
                if(token.length() > 0) {
                    iDrive newDrive;
                    switch(driveName) {//<------------------- TODO: add new drive HERE
                        case "Яндекс.Диск":
                            newDrive = new Yandex(token,this);
                            break;
                        case "Dropbox":
                        default:
                            newDrive = new DropBox(token, this);
                            break;
                    }
                    SharedPreferences prefs = this.getSharedPreferences("group.genco.onecloud", Context.MODE_PRIVATE);
                    Set<String> tokens = prefs.getStringSet(driveName, new HashSet<>());
                    tokens.add(token);
                    SharedPreferences.Editor e = prefs.edit();
                    e.remove(driveName).apply();
                    e.putStringSet(driveName, tokens).commit();

                    cloud.add(newDrive);
                    LoadPath();
                    LoadMenu();
                }
                popUp.dismiss();
            });

            popUp.setContentView(popupView);
            popUp.setBackgroundDrawable(getResources().getDrawable(android.R.color.background_light));
            popUp.setFocusable(true);
            popUp.showAtLocation(listView, Gravity.CENTER, 0,0);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        if (v.getId() == R.id.listView) {
            ListView lv = (ListView) v;
            AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
            File file = (File) lv.getItemAtPosition(acmi.position);
            if("..".equals(file.getName()))
                return;

            //menu.setHeaderTitle(file.getName());
            if(file.getType() != 0)
                menu.add(0, "Скачать".hashCode(),0, "Скачать");
            menu.add(0, "Переименовать".hashCode(),0, "Переименовать");
            menu.add(0, "Удалить".hashCode(),0, "Удалить");
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        File file = (File)listView.getItemAtPosition(info.position);
        String fP = String.join("/", fullPath) + "/" + file.getName();
        if("Скачать".hashCode() == item.getItemId()) {
            String downloadPath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getPath() + "/" + file.getName();

            cloud.download(fP, downloadPath);//<-----------------------TODO: test

            return true;
        }else if ("Переименовать".hashCode() == item.getItemId()) {
            //TODO: rename File
            return true;
        }else if("Удалить".hashCode() == item.getItemId()) {

            cloud.rm(fP);
            files.remove(file);
            adapter.clear();
            adapter.addAll(files);
            adapter.notifyDataSetChanged();
            return true;
        } else
            return super.onContextItemSelected(item);
    }

    void LoadPath() {
        if(null == adapter) {
            files = new ArrayList<>();
            files.add(new File(0, "..", 0, "", ""));
            adapter = new FileAdapter(this, R.layout.list_view_item, files);
            listView.setAdapter(adapter);
        }
        AsyncFileLoad L = new AsyncFileLoad(cloud, adapter, files);
        L.execute(String.join("/", fullPath));
    }

    void LoadMenu() {
        Menu menu = navigationView.getMenu();
        menu.clear();
        cloud.getDrives().parallelStream().forEach(D -> menu.add(D.getUserName()).setIcon(D.getIcon()));
        menu.add(0, R.id.action_add_cloud, 0, "Добавить").setIcon(android.R.drawable.ic_input_add);//btn Add new Cloud
    }

    void LoadPrefs() {
        SharedPreferences prefs = this.getSharedPreferences("group.genco.onecloud", Context.MODE_PRIVATE);
        Map<String, ?> tokens = prefs.getAll();
        tokens.forEach((K, T) -> {
            Set<String> token = (Set<String>)T;
            if("Яндекс.Диск".equals(K)) {//<------------------- TODO: add new drive HERE
                token.forEach(V -> cloud.add(new Yandex(V, this)));
            } else if("Dropbox".equals(K)) {
                token.forEach(V -> cloud.add(new DropBox(V, this)));
            }
        });

    }



    private static class AsyncFileLoad extends AsyncTask<String, Void, Boolean> {//params, progress, result
        private Cloud _cloud;
        private FileAdapter _adapter;
        private List<File> _files;

        public AsyncFileLoad(Cloud c, FileAdapter a, List<File> f) {
            _cloud = c;
            _adapter = a;
            _files = f;
        }

        @Override
        protected Boolean doInBackground(String... o) {
            String fP = o[0];
            if(_cloud.count() > 0) {
                _files = _cloud.list(fP.replaceAll("//", "/"));
                if (fP.indexOf("/") != fP.lastIndexOf("/"))//_fullPath.size() > 1)
                    _files.add(0, new File(0, "..", 0, "", ""));// up dir
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            _adapter.clear();
            _adapter.addAll(_files);
            _adapter.notifyDataSetChanged();
        }
    }
}
