package no.idporten.eudiw.connector.authoritativesources.pid;

import no.digdir.freg.domain.json.Folkeregisterfoedsel;
import no.digdir.freg.domain.json.Folkeregisterperson;
import no.digdir.freg.domain.json.Folkeregisterpersonnavn;
import no.digdir.freg.domain.json.Statsborgerskap;

import java.util.Collections;

public class FregTestUtils {

    public static Folkeregisterperson createFolkeregisterperson() {
        return createFolkeregisterperson("2000");
    }

    public static Folkeregisterperson createFolkeregisterperson(String birthYear) {
        Folkeregisterperson fregPerson = new Folkeregisterperson();
        Folkeregisterpersonnavn navn = new Folkeregisterpersonnavn();
        navn.setEtternavn("Etternavnesen");
        navn.setFornavn("Forenavnesen");
        navn.setErGjeldende(true);
        fregPerson.setNavn(Collections.singletonList(navn));
        Folkeregisterfoedsel fodsel = new Folkeregisterfoedsel();
        fodsel.setFoedeland("NOR");
        fodsel.setFoedselsdato(birthYear + "-01-01");
        fodsel.setErGjeldende(true);
        fregPerson.setFoedsel(Collections.singletonList(fodsel));
        Statsborgerskap statsborgerskap = new Statsborgerskap();
        statsborgerskap.setStatsborgerskap("NOR");
        statsborgerskap.setErGjeldende(true);
        fregPerson.setStatsborgerskap(Collections.singletonList(statsborgerskap));
        return fregPerson;
    }
}
