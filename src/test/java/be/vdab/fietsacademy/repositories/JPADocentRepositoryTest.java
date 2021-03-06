package be.vdab.fietsacademy.repositories;

import be.vdab.fietsacademy.domain.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import javax.persistence.EntityManager;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Sql({"/insertCampus.sql", "/insertVerantwoordelijkheid.sql",
        "/insertDocent.sql", "/insertDocentVerantwoordelijkheid.sql"})
@Import(JPADocentRepository.class)
public class JPADocentRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    private final JPADocentRepository jpaDocentRepository;
    private static final String DOCENTEN = "docenten";
    private Docent docent;
    private final EntityManager manager;
    private Campus campus;

    public JPADocentRepositoryTest(JPADocentRepository jpaDocentRepository, EntityManager manager) {
        this.jpaDocentRepository = jpaDocentRepository;
        this.manager = manager;
    }

    private long idVanTestMan(){
        return super.jdbcTemplate.queryForObject(
                "select id from docenten where voornaam = 'testM'", long.class);
    }

    private long idVanTestVrouw(){
        return super.jdbcTemplate.queryForObject(
                "select id from docenten where voornaam = 'testV'", long.class);
    }

    @BeforeEach
    void beforeEach(){
        campus = new Campus("test", new Adres("test", "test", "test", "test"));
        docent = new Docent("test", "test", Geslacht.MAN,
                BigDecimal.valueOf(100), "test@test.be", campus);
//        campus.addDocent(docent);
    }

    @Test
    void verantwoordelijkhedenLezen(){
        assertThat(jpaDocentRepository.findById(idVanTestMan()).get().getVerantwoordelijkheden())
                .containsOnly(new Verantwoordelijkheid("test"));
    }

    @Test
    void verantwoordelijkheidToevoegen(){
        var verantwoordelijkheid = new Verantwoordelijkheid("test2");
        manager.persist(verantwoordelijkheid);
        manager.persist(campus);
        jpaDocentRepository.create(docent);
        docent.add(verantwoordelijkheid);
        manager.flush();
        assertThat(super.jdbcTemplate.queryForObject(
                "select verantwoordelijkheidid from docentenverantwoordelijkheden " +
                "where docentid = ?", Long.class, docent.getId()).longValue())
                .isEqualTo(verantwoordelijkheid.getId());
    }

    @Test
    void findById(){
        System.out.println(idVanTestMan());
        jpaDocentRepository.findById(idVanTestMan());
        assertThat(jpaDocentRepository.findById(idVanTestMan()).get().getVoornaam())
                .isEqualTo("testM");
    }

    @Test
    void findByOnbestaandeId(){
        assertThat(jpaDocentRepository.findById(-1)).isNotPresent();
    }

    @Test
    void man(){
        assertThat(jpaDocentRepository.findById(idVanTestMan())
        .get().getGeslacht()).isEqualTo(Geslacht.MAN);
    }

    @Test
    void vrouw(){
        assertThat(jpaDocentRepository.findById(idVanTestVrouw())
                .get().getGeslacht()).isEqualTo(Geslacht.VROUW);
    }

    @Test
    void create(){
        manager.persist(campus);
        jpaDocentRepository.create(docent);
        manager.flush();
        assertThat(docent.getId()).isPositive();
        assertThat(super.countRowsInTableWhere(DOCENTEN, "id = " + docent.getId())).isOne();
        assertThat(super.jdbcTemplate.queryForObject(
                "select campusid from docenten where id = ?", Long.class, docent.getId()
        )).isEqualTo(campus.getId());
        // Test om aan te tonen dat je beter geen generated id als hash code check gebruikt ; lukt wel als emailadres gebruikt wordt
        assertThat(campus.getDocenten().contains(docent)).isTrue();
    }

    @Test
    void delete(){
        var id = idVanTestMan();
        jpaDocentRepository.delete(id);
        manager.flush();
        assertThat(super.countRowsInTableWhere(DOCENTEN, "id = " + id)).isZero();
    }

    @Test
    void findAll(){
        assertThat(jpaDocentRepository.findAll())
                .hasSize(super.countRowsInTable(DOCENTEN))
                .extracting(docent -> docent.getWedde())
                .isSorted();
    }

    @Test
    void findByWeddeBetween(){
        var duizend = BigDecimal.valueOf(1000);
        var tweeduizend = BigDecimal.valueOf(2000);
        var docenten = jpaDocentRepository.findByWeddeBetween(duizend, tweeduizend);
        manager.clear();
        assertThat(docenten)
                .hasSize(super.countRowsInTableWhere(DOCENTEN, "wedde between 1000 and 2000"))
                .allSatisfy(docent -> assertThat(docent.getWedde()).isBetween(duizend, tweeduizend));
        assertThat(docenten).extracting(docent -> docent.getCampus().getNaam());
    }

    @Test
    void findAllEmails(){
        assertThat(jpaDocentRepository.findAllEmail()).hasSize(super.countRowsInTable(DOCENTEN));
    }

    @Test
    void algemeneOpslag(){
        assertThat(jpaDocentRepository.algemeneOpslag(BigDecimal.TEN))
                .isEqualTo(super.countRowsInTable(DOCENTEN));
        assertThat(super.jdbcTemplate.queryForObject(
                "select wedde from docenten where id = ?", BigDecimal.class, idVanTestMan()))
                .isEqualByComparingTo("1100");
    }

    @Test
    void bijnamenLezen() {
        assertThat(jpaDocentRepository.findById(idVanTestMan()).get().getBijnamen())
                .containsOnly("test");
    }
    @Test
    void bijnaamToevoegen() {
        manager.persist(campus);
        jpaDocentRepository.create(docent);
        docent.addBijnaam("test");
        manager.flush();
        assertThat(super.jdbcTemplate.queryForObject(
                "select bijnaam from docentenbijnamen where docentid=?", String.class,
                docent.getId()))
                .isEqualTo("test");
    }

    @Test
    void campusLazyLoaded(){
        var docent = jpaDocentRepository.findById(idVanTestMan()).get();
        assertThat(docent.getCampus().getNaam()).isEqualTo("test");
    }
}
