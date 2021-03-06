package be.vdab.fietsacademy.domain;

import javax.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "docenten")
@NamedEntityGraph(name = "Docent.metCampusEnVerantwoordelijkheden",
        attributeNodes = {@NamedAttributeNode("campus"), @NamedAttributeNode("verantwoordelijkheden")})
public class Docent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private String voornaam;
    private String familienaam;
    @Enumerated(EnumType.STRING)
    private Geslacht geslacht;
    private BigDecimal wedde;
    private String emailAdres;
    @ElementCollection
    @CollectionTable(name = "docentenbijnamen", joinColumns = @JoinColumn(name = "docentid"))
    @Column(name = "bijnaam")
    private Set<String> bijnamen;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "campusid")
    private Campus campus;
    @ManyToMany(mappedBy = "docenten")
    private Set<Verantwoordelijkheid> verantwoordelijkheden = new LinkedHashSet<>();
    @Version
    private Timestamp versie;

    public Docent(String voornaam, String familienaam, Geslacht geslacht,
                  BigDecimal wedde, String emailAdres, Campus campus) {
        this.voornaam = voornaam;
        this.familienaam = familienaam;
        this.geslacht = geslacht;
        this.wedde = wedde;
        this.emailAdres = emailAdres;
        this.bijnamen = new LinkedHashSet<>();
        setCampus(campus);
    }

    protected Docent() {
    }

    @Override
    public boolean equals(Object object) {
        if (object instanceof Docent) {
            var andereDocent = (Docent) object;
            return this.emailAdres.equalsIgnoreCase(andereDocent.emailAdres);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return emailAdres == null ? 0 : emailAdres.toLowerCase().hashCode();
    }

    public Campus getCampus() {
        return campus;
    }

    public void setCampus(Campus campus) {
        if (!campus.getDocenten().contains(this)){
            campus.addDocent(this);
        }
        this.campus = campus;
    }

    public Set<String> getBijnamen(){
        return Collections.unmodifiableSet(bijnamen);
    }

    public boolean addBijnaam(String bijnaam){
        if (bijnaam.trim().isEmpty()) {
            throw new IllegalArgumentException();
        }
        return bijnamen.add(bijnaam);
    }

    public boolean removeBijnaam(String bijnaam){
        return bijnamen.remove(bijnaam);
    }

    public long getId() {
        return id;
    }

    public Geslacht getGeslacht() {
        return geslacht;
    }

    public String getVoornaam() {
        return voornaam;
    }

    public String getFamilienaam() {
        return familienaam;
    }

    public BigDecimal getWedde() {
        return wedde;
    }

    public String getEmailAdres() {
        return emailAdres;
    }

    public void opslag(BigDecimal percentage){
        if (percentage.compareTo(BigDecimal.ZERO) <= 0){
            throw new IllegalArgumentException();
        }
        var factor = BigDecimal.ONE.add(percentage.divide(BigDecimal.valueOf(100)));
        wedde = wedde.multiply(factor).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean add(Verantwoordelijkheid verantwoordelijkheid){
        var toegevoegd = verantwoordelijkheden.add(verantwoordelijkheid);
        if (!verantwoordelijkheid.getDocenten().contains(this)){
            verantwoordelijkheid.add(this);
        }
        return toegevoegd;
    }

    public boolean remove(Verantwoordelijkheid verantwoordelijkheid){
        var verwijderd = verantwoordelijkheden.remove(verantwoordelijkheid);
        if (verantwoordelijkheid.getDocenten().contains(this)){
            verantwoordelijkheid.remove(this);
        }
        return verwijderd;
    }

    public Set<Verantwoordelijkheid> getVerantwoordelijkheden(){
        return Collections.unmodifiableSet(verantwoordelijkheden);
    }
}
