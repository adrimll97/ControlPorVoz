package es.multimedia.controlporvoz;

import android.support.annotation.NonNull;

public class Contacto implements Comparable<Contacto>{
    String nombre;
    String telefono;

    public Contacto(String nombre, String telefono){
        this.nombre = nombre;
        this.telefono = telefono;
    }
    @Override
    public int compareTo(@NonNull Contacto c) {
        return nombre.compareTo(c.nombre);
    }
}
