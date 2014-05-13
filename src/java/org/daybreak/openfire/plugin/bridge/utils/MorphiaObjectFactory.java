package org.daybreak.openfire.plugin.bridge.utils;

import com.mongodb.DBObject;
import org.mongodb.morphia.mapping.DefaultCreator;
import org.mongodb.morphia.mapping.Mapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by alan on 14-5-13.
 */
public class MorphiaObjectFactory extends DefaultCreator {

    private static final Logger logger = LoggerFactory.getLogger(MorphiaObjectFactory.class);

    @Override
    public Object createInstance(final Class clazz, final DBObject dbObj) {
        Class c = getClass(dbObj);
        if (c == null) {
            c = clazz;
        }
        return createInstance(c);
    }

    private Class getClass(final DBObject dbObj) {
        // see if there is a className value
        final String className = (String) dbObj.get(Mapper.CLASS_NAME_FIELDNAME);
        Class c = null;
        if (className != null) {
            // try to Class.forName(className) as defined in the dbObject first,
            // otherwise return the entityClass
            try {
                c = Class.forName(className);
            } catch (ClassNotFoundException e) {
                logger.warn("Class not found defined in dbObj: ", e);
            }
        }
        return c;
    }
}
