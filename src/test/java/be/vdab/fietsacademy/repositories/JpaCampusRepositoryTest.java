package be.vdab.fietsacademy.repositories;

import be.vdab.fietsacademy.domain.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.junit4.AbstractTransactionalJUnit4SpringContextTests;

import javax.persistence.EntityManager;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(JpaCampusRepository.class)
@Sql({"/insertCampus.sql", "/insertDocent.sql"})
public class JpaCampusRepositoryTest extends AbstractTransactionalJUnit4SpringContextTests {
    private static final String CAMPUSSEN = "campussen";
    private final JpaCampusRepository repository;

    private final EntityManager manager;

    public JpaCampusRepositoryTest(JpaCampusRepository repository, EntityManager manager) {
        this.repository = repository;
        this.manager = manager;
    }

    private Long idVanTestCampus() {
        return super.jdbcTemplate.queryForObject(
                "select id from campussen where naam='test'", Long.class);
    }

    @Test
    void docentenLazyLoaded(){
        assertThat(repository.findById(idVanTestCampus()).get().getDocenten())
                .hasSize(2)
                .first().extracting(docent -> docent.getVoornaam()).isEqualTo("testM");
    }

    @Test
    void findById(){
        var campus = repository.findById(idVanTestCampus()).get();
        assertThat(campus).isInstanceOf(Campus.class);
        assertThat(campus.getNaam()).isEqualTo("test");
        assertThat(campus.getAdres().getGemeente()).isEqualTo("test");
    }

    @Test
    void findByOnbestaandeId(){
        assertThat(repository.findById(-1)).isNotPresent();
    }

    @Test
    void createCampus(){
        var campus = new Campus("test", new Adres("test", "test", "test","test"));
        repository.create(campus);
        assertThat(super.countRowsInTableWhere(CAMPUSSEN,
                "id = " + campus.getId() )).isOne();
    }

    @Test
    void telefoonNrsLezen(){
        assertThat(repository.findById(idVanTestCampus()).get().gettelefoonNrs())
                .containsOnly(new TelefoonNr("052334625", false, "test"));
        repository.findById(idVanTestCampus()).get().gettelefoonNrs().stream()
                .forEach(telefoonNr -> {
                    System.out.print("telefoonnr is : ");
                    System.out.println(telefoonNr.getNummer());
                    System.out.print(telefoonNr.getOpmerking());
                    System.out.println("einde");

                });
    }
}
