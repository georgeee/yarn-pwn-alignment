package ru.georgeee.bachelor.yarn.db;

import org.hibernate.Hibernate;
import org.hibernate.proxy.HibernateProxy;

import java.io.*;
import java.util.function.Function;

public class HibernateHelper {
    private HibernateHelper() {
    }

    public static <T> void initialize(T object, Function<T, ?>... fieldMappers) {
        if (object == null) return;
        for (Function<T, ?> mapper : fieldMappers) {
            Hibernate.initialize(mapper.apply(object));
        }
    }

    public static byte[] serialize(Object object) {
        if (object == null) {
            return null;
        }
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ObjectOutputStream out = new ObjectOutputStream(baos)) {
            out.writeObject(object);
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends Serializable> T deserialize(byte[] bytes) {
        if (bytes == null) {
            return null;
        }
        try (ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
             ObjectInputStream input = new ObjectInputStream(bais)) {
            return (T) input.readObject();
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> T unproxy(T object) {
        if (object instanceof HibernateProxy) {
            return (T) (((HibernateProxy) object).getHibernateLazyInitializer().getImplementation());
        }
        return object;
    }
}
