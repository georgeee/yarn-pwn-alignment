package ru.georgeee.bachelor.yarn.db;

import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.hibernate.jpa.HibernateEntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.georgeee.bachelor.yarn.db.entity.Synset;

import java.util.Collection;
import java.util.List;

@Service
public class CommonDbService {
    @Autowired
    private ApplicationContext context;

    @SuppressWarnings("unchecked")
    @Transactional
    public List<Synset> getOrphans(Collection<String> synsetIds) {
        HibernateEntityManager entityManager = context.getBean(HibernateEntityManager.class);
        Session session = entityManager.getSession();
        return session.createCriteria(Synset.class)
                .add(Restrictions.in("externalId", synsetIds))
                .add(Restrictions.eq("edgeCount", 0))
                .list();
    }
}
