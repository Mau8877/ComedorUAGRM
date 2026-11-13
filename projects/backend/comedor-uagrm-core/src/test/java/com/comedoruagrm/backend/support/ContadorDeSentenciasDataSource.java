package com.comedoruagrm.backend.support;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import org.springframework.jdbc.datasource.DelegatingDataSource;

/**
 * `DataSource` que cuenta las sentencias SQL que se preparan, solo las del hilo que las pide, para probar que
 * una operacion hace un numero constante de consultas sin importar cuantas filas toque (sin N+1).
 *
 * * Cuenta por hilo a proposito: el despacho de push y correo corre despues del commit en otro hilo y no debe
 * * mezclarse con lo que hace la peticion. Un test lo instala con un `BeanPostProcessor` y usa
 * * `reiniciar()` / `sentencias()` desde su propio hilo (el de `MockMvc`).
 */
public class ContadorDeSentenciasDataSource extends DelegatingDataSource {

    private static final ThreadLocal<int[]> SENTENCIAS = ThreadLocal.withInitial(() -> new int[1]);

    public ContadorDeSentenciasDataSource(DataSource destino) {
        super(destino);
    }

    /** Pone en cero el contador del hilo actual. */
    public static void reiniciar() {
        SENTENCIAS.get()[0] = 0;
    }

    /** Sentencias preparadas por el hilo actual desde el ultimo `reiniciar()`. */
    public static int sentencias() {
        return SENTENCIAS.get()[0];
    }

    @Override
    public Connection getConnection() throws SQLException {
        return contar(super.getConnection());
    }

    @Override
    public Connection getConnection(String usuario, String clave) throws SQLException {
        return contar(super.getConnection(usuario, clave));
    }

    private static Connection contar(Connection real) {
        return (Connection) Proxy.newProxyInstance(Connection.class.getClassLoader(),
                new Class<?>[] {Connection.class}, (proxy, metodo, args) -> {
                    String nombre = metodo.getName();
                    if (nombre.startsWith("prepare") || nombre.equals("createStatement")) {
                        SENTENCIAS.get()[0]++;
                    }
                    try {
                        return metodo.invoke(real, args);
                    } catch (InvocationTargetException e) {
                        throw e.getCause();
                    }
                });
    }
}
