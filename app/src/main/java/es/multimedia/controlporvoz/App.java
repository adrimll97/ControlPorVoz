package es.multimedia.controlporvoz;

import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;

public class App implements Comparable<App>{
    String paquete;
    CharSequence nombre;
    Drawable icono;

    public App(String paquete, CharSequence nombre, Drawable icono) {
        this.paquete = paquete;
        this.nombre = nombre;
        this.icono = icono;
    }

    @Override
    public int compareTo(@NonNull App a) {
        String n1 = nombre.toString();
        String n2 = a.nombre.toString();
        return n1.compareTo(n2);
    }
}
