package uk.co.synatronics.Datafusion;

import static uk.co.synatronics.Datafusion.FileUtils.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;

public class CurrencyConverter {

    final String from, to;
    final double amount;
    private double convertedValue;

    protected CurrencyConverter(String from, String to, double amount) {
        this.from = from;
        this.to = to;
        this.amount = amount;
    }

    protected CurrencyConverter (URL url) throws IllegalArgumentException {
        try {
            String parameters = url.toString().substring(43);
            int fromIndex = parameters.indexOf('&'),
                toIndex = parameters.indexOf('&', fromIndex)+10;
            this.amount = Double.parseDouble(parameters.substring(2, fromIndex));
            fromIndex += 6; toIndex += 3;
            this.from = parameters.substring(fromIndex, fromIndex+3);
            this.to = parameters.substring(toIndex);
        }
        catch (Exception ex) {
            System.err.println("Couldn't parse currency conversion parameters from \""+url+'"');
            System.err.println(ex.getMessage());
            throw new IllegalArgumentException("Incorrect URL specified: "+url);
        }
    }

    protected CurrencyConverter(String params) throws IllegalArgumentException {
        if (params == null || params.length() < 7)
            throw new IllegalArgumentException("Incorrect arguments for currency converter: \""+params+'"');

        this.from = params.substring(0, 3);
        this.to = params.substring(4, 7);
        double tempAmnt = 1;
        try {
            tempAmnt = Double.parseDouble(params.substring(8));
        }
        catch (Exception ex) {
            System.err.println("Couldn't parse currency amount: "+ex.getMessage());
        }
        this.amount = tempAmnt;
    }

    protected double convert() {
        if (convertedValue <= 0)
            convertedValue = convertCurrency(from, to, amount);
        return convertedValue;
    }

    protected static double convertCurrency (String from, String to, double amount) {
        return findCurrencyValue(readURL(getCurrencyConverterService(from, to, amount)));
    }

    private static double findCurrencyValue(String html) {
        String value = find(html, "<span class=bld>", 1, 6);
        try {
            return Double.parseDouble(new DecimalFormat("#.##").format(Double.valueOf(value)));
        }
        catch (NumberFormatException nf) {
            System.err.println("Couldn't find currency value in expected location. Got this instead: "+value);
            System.err.println(nf.getMessage());
            return -1;
        }
    }

    protected static URL getCurrencyConverterURL (String from, String to, double amount) {
        try {
            return new URL(getCurrencyConverterService(from, to, amount));
        }
        catch (MalformedURLException mue) {
            System.err.println(mue.getMessage());
            return null;
        }
    }

    protected static String getCurrencyConverterService(String from, String to, double amount) {
        return "https://www.google.co.uk/finance/converter?a="+amount+"&from="+from+"&to="+to;
    }
}
