package com.comedoruagrm.backend.support;

import java.util.concurrent.atomic.AtomicInteger;

import javax.sql.DataSource;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.StatementCallback;

/**
 * `JdbcTemplate` que cuenta cuantas sentencias ejecuta, para probar que una operacion hace un numero
 * constante de consultas sin importar cuantas filas toque (sin N+1). Usa el mismo `DataSource` que el resto
 * del test, asi participa de la misma transaccion.
 */
public class JdbcContadorDeConsultas extends JdbcTemplate {

    private final AtomicInteger consultas = new AtomicInteger();

    public JdbcContadorDeConsultas(DataSource dataSource) {
        super(dataSource);
    }

    // Spring 7 resuelve las consultas con un `execute` interno privado: se cuentan en la puerta publica.
    @Override
    public <T> T query(PreparedStatementCreator psc, PreparedStatementSetter pss, ResultSetExtractor<T> rse) {
        consultas.incrementAndGet();
        return super.query(psc, pss, rse);
    }

    @Override
    public <T> T execute(PreparedStatementCreator psc, PreparedStatementCallback<T> action) {
        consultas.incrementAndGet();
        return super.execute(psc, action);
    }

    @Override
    public <T> T execute(StatementCallback<T> action) {
        consultas.incrementAndGet();
        return super.execute(action);
    }

    public int consultas() {
        return consultas.get();
    }

    public void reiniciar() {
        consultas.set(0);
    }
}
