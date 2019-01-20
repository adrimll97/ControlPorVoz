package es.multimedia.controlporvoz;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.Image;
import android.net.Uri;
import android.provider.ContactsContract;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringTokenizer;

public class MainActivity extends AppCompatActivity {
    private TextToSpeech textToSpeech;
    private static final int RequestPermissionCode  = 1 ;
    private static final int REQ_CODE_SPEECH_INPUT = 100;
    private TextView mEntradaVoz;
    private ImageButton mBotonHablar;

    private PackageManager packageManager;
    private List<App> apps;

    Cursor cursor;
    private List<Contacto> contactos;

    private ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBotonHablar = findViewById(R.id.botonHablar);

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                    Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, RequestPermissionCode);
        } else {
            loadContacts();
        }
        mBotonHablar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                iniciarVoz();
            }
        });
        loadApps();
        textToSpeech = new TextToSpeech(this, new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status == TextToSpeech.SUCCESS){
                    Locale spanish = new Locale("es", "ES");
                    textToSpeech.setLanguage(spanish);
                }
                else Log.e("TTS", "TTS no inicializado");
            }
        });
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
                    String entrada = result.get(0).toLowerCase();
                    StringTokenizer token = new StringTokenizer(entrada, " ");
                    String accion = token.nextToken();
                    //mEntradaVoz.setText(entrada + "\nacción=" + accion + "\ntarget=" + entrada.substring(accion.length()+1));

                    if (token.countTokens() == 0){
                        if (accion.toLowerCase().equals("aplicaciones")){
                            loadListViewApps();
                            addOnClickListenerListApps();
                            textToSpeech.speak("Mostrando todas las aplicaciones instaladas", TextToSpeech.QUEUE_FLUSH, null,null);
                        } else if (accion.toLowerCase().equals("contactos")){
                            loadListViewContacts();
                            textToSpeech.speak("Mostrando todos los contactos", TextToSpeech.QUEUE_FLUSH, null,null);
                        } else {
                            textToSpeech.speak("No reconozco ese comando", TextToSpeech.QUEUE_FLUSH, null,null);
                        }
                    } else {
                        if (accion.toLowerCase().equals("abrir")){
                            String a = entrada.substring(accion.length()+1);
                            Boolean encontrada = Boolean.FALSE;
                            for (App app : apps){
                                if (a.toLowerCase().equals(app.nombre.toString().toLowerCase())) {
                                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.paquete.toString());
                                    if (launchIntent != null) {
                                        encontrada = Boolean.TRUE;
                                        textToSpeech.speak("Abriendo " + a, TextToSpeech.QUEUE_FLUSH, null,null);
                                        startActivity(launchIntent);//null pointer check in case package name was not found
                                    }
                                }
                            }

                            if (encontrada == Boolean.FALSE){
                                textToSpeech.speak("No reconozco la aplicación " + a, TextToSpeech.QUEUE_FLUSH, null,null);
                            }
                        }
                    }

                    /*for (App app : apps){
                        if (result.get(0).toLowerCase().equals(app.nombre.toString().toLowerCase())) {
                            Intent launchIntent = getPackageManager().getLaunchIntentForPackage(app.paquete.toString());
                            if (launchIntent != null) {
                                startActivity(launchIntent);//null pointer check in case package name was not found
                            }
                        }
                    }

                    if (result.get(0).toLowerCase().equals("aplicaciones")) {
                        loadListViewApps();
                        addOnClickListenerListApps();
                        mEntradaVoz.setText("Mostrando todas las aplicaciones instalas");
                    } else if (result.get(0).toLowerCase().equals("contactos")){
                        mEntradaVoz.setText("Mostrando todos los contactos");
                    } else if (result.get(0).toLowerCase().equals("llamar")) {
                        String call = "tel:675615370";
                        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                            ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                                    Manifest.permission.CALL_PHONE}, RequestPermissionCode);
                        }
                        startActivity(new Intent(Intent.ACTION_CALL, Uri.parse(call)));
                    }*/
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
            App app = new App(
                    ri.activityInfo.packageName,
                    ri.loadLabel(packageManager),
                    ri.loadIcon(packageManager)
            );
            apps.add(app);
        }
        Collections.sort(apps);
    }

    private void loadContacts(){
        contactos = new ArrayList<>();
        cursor = getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,null, null, null);

        while(cursor.moveToNext()){
            Contacto contacto = new Contacto(
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)),
                    cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER))
            );
            contactos.add(contacto);
        }
        Collections.sort(contactos);
    }


    private void loadListViewApps(){
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

    private void loadListViewContacts(){
        list = findViewById(R.id.list);
        ArrayAdapter<Contacto> adapter = new ArrayAdapter<Contacto>(this, R.layout.contactos, contactos){
            @NonNull
            @Override
            public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
                if (convertView == null){
                    convertView = getLayoutInflater().inflate(R.layout.contactos, null);
                }
                TextView contactNombre = (TextView) convertView.findViewById(R.id.name);
                contactNombre.setText(contactos.get(position).nombre);

                TextView contactNumber = (TextView) convertView.findViewById(R.id.phoneNumber);
                contactNumber.setText(contactos.get(position).telefono);

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

    private void cargarPermisos() {
        AlertDialog.Builder dialogo = new AlertDialog.Builder(MainActivity.this);
        dialogo.setTitle("Permisos no otorgados");
        dialogo.setMessage("Debe otorgar permisos de acceso a los contactos para el correcto funcionamiento de la aplicación");

        dialogo.setPositiveButton("Aceptar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                ActivityCompat.requestPermissions(MainActivity.this,new String[]{
                        Manifest.permission.READ_CONTACTS, Manifest.permission.CALL_PHONE}, RequestPermissionCode);
            }
        });
        dialogo.show();
    }

    @Override
    public void onRequestPermissionsResult(int RC, String per[], int[] PResult) {

        switch (RC) {

            case RequestPermissionCode:

                if (PResult.length > 0 && PResult[0] == PackageManager.PERMISSION_GRANTED) {
                    loadContacts();
                } else {
                    cargarPermisos();
                }
                break;
        }
    }
}
