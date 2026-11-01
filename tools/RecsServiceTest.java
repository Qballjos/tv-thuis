package io.freemytv;

public final class RecsServiceTest {
    private static void check(boolean ok, String why) {
        if (!ok) throw new AssertionError(why);
    }
    public static void main(String[] args) {
        for (String name : new String[] {"max", "hbo max", "max standard", "hbo max (ad-free)"}) {
            check(ServiceNames.max(name), "Max service recognized: " + name);
        }
        for (String name : new String[] {"", "cinemax", "imax", "maxim", "bmaxb"}) {
            check(!ServiceNames.max(name), "unrelated service excluded: " + name);
        }
        String longName = String.join("", java.util.Collections.nCopies(100_000, "bmaxb"));
        check(!ServiceNames.max(longName), "long nonmatching provider name");
        check(ServiceNames.max(longName + " hbo max"), "Max after long prefix");
        System.out.println("Services: Max/HBO Max, unrelated names and long provider input passed.");
    }
}
