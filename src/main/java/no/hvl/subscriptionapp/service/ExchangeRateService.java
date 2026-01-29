package no.hvl.subscriptionapp.service;

import org.springframework.stereotype.Service;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

/**
 * Henter valutakurser fra ECB (base: EUR) og cacher dagens kurser i minne.
 * ECB format: 1 EUR = rate * CURRENCY (f.eks. USD 1.09).
 */
@Service
public class ExchangeRateService {

    private static final String ECB_DAILY_URL =
            "https://www.ecb.europa.eu/stats/eurofxref/eurofxref-daily.xml";

    private volatile LocalDate cachedDate = null;
    private final Map<String, BigDecimal> eurTo = new ConcurrentHashMap<>();

    /**
     * Konverterer amount fra "fromCurrency" til NOK ved bruk av ECB-kurser.
     * Returnerer null hvis vi mangler kurs.
     */
    public BigDecimal convertToNok(BigDecimal amount, String fromCurrency) {
        if (amount == null) return null;

        String ccy = norm(fromCurrency);
        if ("NOK".equals(ccy)) return amount;

        // sørg for at cache er lastet
        ensureRatesLoaded();

        // ECB base EUR: 1 EUR = eurTo[CCY]
        // NOK = amount / eurTo[CCY] * eurTo[NOK]
        BigDecimal eurToFrom = eurTo.get(ccy);
        BigDecimal eurToNok = eurTo.get("NOK");

        if ("EUR".equals(ccy)) eurToFrom = BigDecimal.ONE;
        if (eurToNok == null) return null;
        if (eurToFrom == null) return null;

        // amount(CCY) -> EUR
        BigDecimal inEur = amount.divide(eurToFrom, 12, RoundingMode.HALF_UP);

        // EUR -> NOK
        BigDecimal inNok = inEur.multiply(eurToNok);

        return inNok.setScale(2, RoundingMode.HALF_UP);
    }

    private String norm(String c) {
        if (c == null || c.isBlank()) return "NOK";
        return c.trim().toUpperCase();
    }

    private void ensureRatesLoaded() {
        LocalDate today = LocalDate.now();
        if (today.equals(cachedDate) && !eurTo.isEmpty()) return;

        synchronized (this) {
            if (today.equals(cachedDate) && !eurTo.isEmpty()) return;
            loadFromEcb();
            cachedDate = today;
        }
    }

    private void loadFromEcb() {
        try (InputStream in = new URL(ECB_DAILY_URL).openStream()) {

            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .parse(in);

            // ECB XML har mange "Cube"-noder; de vi vil ha er de med attributter currency/rate
            NodeList cubes = doc.getElementsByTagName("Cube");

            eurTo.clear();
            eurTo.put("EUR", BigDecimal.ONE);

            for (int i = 0; i < cubes.getLength(); i++) {
                var node = cubes.item(i);
                var attrs = node.getAttributes();
                if (attrs == null) continue;

                var currencyAttr = attrs.getNamedItem("currency");
                var rateAttr = attrs.getNamedItem("rate");
                if (currencyAttr == null || rateAttr == null) continue;

                String ccy = currencyAttr.getNodeValue().trim().toUpperCase();
                String rateStr = rateAttr.getNodeValue().trim();

                try {
                    eurTo.put(ccy, new BigDecimal(rateStr));
                } catch (Exception ignore) {
                    // hopp over rare verdier
                }
            }

        } catch (Exception e) {
            // Hvis ECB feiler: behold eksisterende cache (hvis finnes)
            // (I MVP er dette ok; senere kan vi vise varsel i UI)
        }
    }
}
