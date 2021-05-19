package com.example.lanzasms;

import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    // String -> tipo de entrada que va a recibir en el método launch()

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private static final String CHANNEL_ID = "canal_SMS";
    private static final int idNotificacionSMS = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button botonSMS = (Button) findViewById(R.id.botonSMS);
        botonSMS.setOnClickListener(this);
        // Lo crea en el caso de que no exista. En otro caso, se ignora el código
        crearCanalDeNotificacion();

        // registerForActivityResult -> recibe RequestPermission y ActivityResultCallback<Boolean>

        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), new ActivityResultCallback<Boolean>() {
            @Override
            public void onActivityResult(Boolean result) {
                if(result){
                    // El permiso ha sido concedido
                    // Llamamos al método que utiliza dichos permisos

                    Log.d("Events","Estamos en onActivityResult true");
                }
                else{

                    // Explicar al usuario que la acción a realizar no está disponible
                    // porque requiere de un permiso que ha denegado. Hay que respetar la
                    // decisión del usuario, así que no le spamees hasta que lo acepte.

                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(MainActivity.this);
                    alertDialogBuilder.setTitle("Permiso denegado correctamente...");
                    alertDialogBuilder.setMessage("Sin este permiso no se podrá mandar el SMS");
                    alertDialogBuilder.setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                        @Override
                        // dialog -> dialogo actual
                        // which -> botón pulsado: DialogInterface.BUTTON_POSITIVE
                        public void onClick(DialogInterface dialog, int which) {

                            Toast.makeText(MainActivity.this,"Descarga cancelada...",Toast.LENGTH_SHORT).show();

                        }
                    });

                    alertDialogBuilder.create().show();
                    Log.d("Events","Estamos en onActivityResult false");

                }
            }
        });
    }

    public void mandarSMS(){

        EditText editText = (EditText) findViewById(R.id.numeroTelefono);
        String destination = editText.getText().toString();

        SmsManager smsManager = SmsManager.getDefault();
        // creamos un número aleatorio de 9 cifras (entre 0 y 9)

        String mensaje = "El código de seguridad es el siguiente: ";

        for(int i = 0; i<10; i++){
            mensaje = mensaje + (int) Math.floor(Math.random()*9+1);
        }

        smsManager.sendTextMessage(destination,null,mensaje,null,null);
        lanzarNotificacion(destination,mensaje);

    }


    public void crearCanalDeNotificacion(){

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            CharSequence nombre = "Notificaciones SMS";
            String descripcion = "Notificaciones de la aplicación de PMDM para SMS";
            int importancia = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel canal = new NotificationChannel(CHANNEL_ID,nombre,importancia);
            canal.setDescription(descripcion);

            // Registramos el canal. No puede ser modificado después

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(canal);

        }

    }

    public void lanzarNotificacion(String destination, String mensaje){

        NotificationCompat.Builder builder = null;

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            // Constructor de notificaciones
            builder = new NotificationCompat.Builder(this,CHANNEL_ID);
        }
        else{
            builder = new NotificationCompat.Builder(this);
        }

        // Creamos el PendingIntent IMPLÍCITO correspondiente
        // En primer lugar, creamos el intent con la acción de mandar un mensaje SMS.

        Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
        smsIntent.setData(Uri.parse("smsto:"+destination));
        smsIntent.putExtra("sms_body",mensaje);

        // Código para comprobar a priori el correcto funcionamiento del intent
        /*
        if(smsIntent.resolveActivity(getPackageManager()) != null){
            startActivity(smsIntent);
        }
        else{
            Toast.makeText(this,"NO FUNCIONA",Toast.LENGTH_SHORT).show();
        }*/

        PendingIntent pendingIntent = PendingIntent.getActivity(this,2,smsIntent,PendingIntent.FLAG_ONE_SHOT);

        builder.setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("¿Quieres lanzar un Intent de SMS?")
                .setContentText("Pulsa para lanzarlo")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this);
        notificationManagerCompat.notify(idNotificacionSMS,builder.build());

    }


    @Override
    public void onClick(View v) {

        // solo tenemos un botón, por lo que no tenemos que comprobar el ID
        if(ContextCompat.checkSelfPermission(this,Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED){
            // llamamos al método de mandar SMS
            mandarSMS();
        }else{
            requestPermissionLauncher.launch(Manifest.permission.SEND_SMS);
        }
    }
}