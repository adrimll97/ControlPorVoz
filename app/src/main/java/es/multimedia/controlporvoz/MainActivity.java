package es.multimedia.controlporvoz;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.media.Image;
import android.net.Uri;
import android.speech.RecognizerIntent;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView mEntradaVoz;
    private ImageButton mBotonHablar;

    private PackageManager packageManager;
    private List<App> apps;
    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mEntradaVoz = findViewById(R.id.textoEntrada);
        mBotonHablar = findViewById(R.id.botonHablar);

        mBotonHablar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarVoz();
            }
        });
        loadApps();
    }

    private void iniciarVoz() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es_ES");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "¿En que puedo ayudarte?");
        try {
            startActivityForResult(intent, REQ_CODE_SPEECH_INPUT);
        } catch (ActivityNotFoundException e) {

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_SPEECH_INPUT: {
                if (resultCode == RESULT_OK && data != null) {
                    ArrayList<String> result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

                    for (App app : apps){
                        if (result.get(0).toLowerCase().equals(app.nombre.toString().toLowerCase())) {
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.paquete.toString());
                            if (launchIntent != null) {
                                startActivity(launchIntent);//null pointer check in case package name was not found
                            }
                        }
                    }

                    if (result.get(0).toLowerCase().equals("aplicaciones")) {
                        loadListView();
                        addOnClickListenerListApps();
                        mEntradaVoz.setText("Mostrando todas las aplicaciones instalas");
                    } else if (result.get(0).toLowerCase().equals("llamar")) {
                        String call = "tel:675615370";
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            // TODO: Consider calling
                            //    ActivityCompat#requestPermissions
                            // here to request the missing permissions, and then overriding
                            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                            //                                          int[] grantResults)
                            // to handle the case where the user grants the permission. See the documentation
                            // for ActivityCompat#requestPermissions for more details.
                            return;
                        }
                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(call)));
                    }
                    //mEntradaVoz.setText(result.get(0).toLowerCase());
                }
                break;
            }
        }
    }

    private void loadApps(){
        packageManager = getPackageManager();
        apps = new ArrayList<>();

        Intent i = new Intent(Intent.ACTION_MAIN, null);
        i.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> disponible = packageManager.queryIntentActivities(i, 0);
        for (ResolveInfo ri : disponible){
            App app = new App();
            app.paquete = ri.activityInfo.packageName;
            app.nombre = ri.loadLabel(packageManager);
            app.icono = ri.loadIcon(packageManager);
            apps.add(app);
        }
    }

    private void loadListView(){
        list = findViewById(R.id.list);
        ArrayAdapter<App> adapter = new ArrayAdapter<App>(this, R.layout.apps, apps){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null){
                    convertView =  getLayoutInflater().inflate(R.layout.apps, null);
                }
                ImageView appIcon = (ImageView) convertView.findViewById(R.id.icon);
                appIcon.setImageDrawable(apps.get(position).icono);

                TextView appNombre = (TextView) convertView.findViewById(R.id.name);
                appNombre.setText(apps.get(position).nombre);

                return convertView;
            }
        };

        list.setAdapter(adapter);
    }

    private void addOnClickListenerListApps(){
        list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = packageManager.getLaunchIntentForPackage(apps.get(position).paquete.toString());
                startActivity(i);
            }
        });
    }
}
