import java.io.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Report {

    private String dataFile;
    private BigDecimal taxRate;
    private final String DEFAULT_SEPARATOR = ",";

    public Report(String dataFile, BigDecimal taxRate){
        this.dataFile = dataFile;
        this.taxRate = taxRate;
    }

    public String getDataFile() {
        return dataFile;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    /**
     * Function to map vehicle data to an object
     */
    private Function<String, Vehicle> mapVehicle = (line) -> {
        String[] attrs = line.split(this.DEFAULT_SEPARATOR,-1);

        int year = -1;
        String make = "";
        String model = "";
        BigDecimal msrp = BigDecimal.ONE.negate();


        try{
            make = attrs[1];
            model = attrs[2];

            if(!(attrs[0].isEmpty() || attrs[3].isEmpty())){
                year = Integer.parseInt(attrs[0]);
                msrp = new BigDecimal(attrs[3]);
            }

        } catch(NumberFormatException e){
            e.printStackTrace();
        }
        return new Vehicle(year, make, model, msrp);
    };

    /**
     * Read Vehicle data from csv and add objects to list.
     * @param dataFile - the input csv file
     * @return vehicles - a list containing Vehicle objects
     */
    private List<Vehicle> parseData(String dataFile) {
        List<Vehicle> vehicles = new ArrayList<>();

        try{
            File file = new File(dataFile);
            InputStream inputStream = new FileInputStream(file);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));

            vehicles = bufferedReader.lines().skip(1).map(mapVehicle).collect(Collectors.toList()); // skip header
            bufferedReader.close();
        } catch(Exception e){
            e.printStackTrace();
        }

        return vehicles;
    }

    /**
     * Aggregate vehicles by year.
     * @param vehicles list of all vehicles read from file
     * @return HashMap containing Year - Vehicles pairs
     */
    private HashMap<Integer, TreeSet<Vehicle>> aggregateVehicles(List<Vehicle> vehicles){
        /* Streams are convenient but not as efficient */

        HashMap<Integer, TreeSet<Vehicle>> map = new HashMap<>();

        for(Vehicle v: vehicles){
            if(!(map.containsKey(v.getYear()))){ // key does not yet exist
                //TreeSet is more useful for maintaining order compared to ArrayList
                TreeSet<Vehicle> yearVehicles = new TreeSet<>(Comparator.comparing(Vehicle::getMake).thenComparing(Vehicle::getId));
                yearVehicles.add(v);
                map.put(v.getYear(), yearVehicles);
            } else{ // simply add to existing set if key exists
                map.get(v.getYear()).add(v);
            }
        }

        return map;

    }

    /**
     * Given the vehicles aggregated by year, print report to a txt file.
     * @param aggregationMap - map containing vehicles grouped and ordered.
     */
    private void generateReport(HashMap<Integer, TreeSet<Vehicle>> aggregationMap){
        try {
            String date = new SimpleDateFormat("MM/dd/yyyy").format(new Date());
            String fileDate = new SimpleDateFormat("MMddyy").format(new Date());
            String fileName = "vehicles" + fileDate + ".txt";
            File outFile = new File(fileName);

            BufferedWriter bufferedWriter;

            BigDecimal totMsrp = BigDecimal.ZERO;
            BigDecimal totListPrice = BigDecimal.ZERO;

            String headerFormatStr = "%-20s %-15s";
            String detailFormatStr = "\t%-20s %-15s %-15s";
            String summaryFormatString = "%-20s";

            if (outFile.createNewFile()) {
                bufferedWriter = new BufferedWriter(new FileWriter(outFile));

                // header section
                bufferedWriter.write(String.format(headerFormatStr, "--- Vehicle Report ---", "  Date: " + date));
                bufferedWriter.newLine();

                // detail section - erroneous data marked as 'invalid'
                for(Map.Entry<Integer, TreeSet<Vehicle>> entry : aggregationMap.entrySet()){
                    if(entry.getKey() == -1){
                        bufferedWriter.write("invalid");
                    } else {
                        bufferedWriter.write(String.valueOf(entry.getKey()));
                    }

                    bufferedWriter.newLine();
                    for (Vehicle v : entry.getValue()) {
                        try {
                            totMsrp = totMsrp.add(v.getMsrp());
                            totListPrice = totListPrice.add(v.getMsrp().multiply(this.getTaxRate()));

                            String make = v.getMake() == null ? "invalid" : v.getMake();
                            String model = v.getModel() == null ? "invalid" : v.getModel();

                            bufferedWriter.write(String.format(detailFormatStr, make + " " + model,
                                    "MSRP: " + (v.getMsrp().equals(BigDecimal.ONE.negate()) ? "invalid" : "$" + v.getMsrp().setScale(2, RoundingMode.HALF_EVEN)),
                                    "List Price: " + (v.getMsrp().equals(BigDecimal.ONE.negate()) ? "invalid" :  "$" +
                                                      v.getMsrp().multiply(this.getTaxRate()).setScale(2, RoundingMode.HALF_EVEN))));
                            bufferedWriter.newLine();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }

                // summary section
                bufferedWriter.write("--- Grand Total ---");
                bufferedWriter.newLine();
                bufferedWriter.write(String.format(summaryFormatString, "\tMSRP: $" + totMsrp.setScale(2, RoundingMode.HALF_EVEN)));
                bufferedWriter.newLine();
                bufferedWriter.write(String.format(summaryFormatString, "\tList Price: $" + totListPrice.setScale(2, RoundingMode.HALF_EVEN)));
                bufferedWriter.close();
            } else {
                System.out.println("File already exists.");
            }
        } catch (IOException e){
            e.printStackTrace();
        }
    }

    /**
     * Test that the vehicles were loaded correctly by printing them.
     * @param vehicles - the list containing Vehicle objects
     */
    private static void printVehicles(List<Vehicle> vehicles){
        for(Vehicle v: vehicles){
            System.out.println(v.getYear() + " | " + v.getMake() + " | " + v.getModel() + " | " + v.getMsrp());
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter Data File: ");
        String dataFile = scanner.nextLine();
        if(dataFile.isEmpty()){
            dataFile = "vehicles.csv";
        }

        System.out.println("Enter Tax Rate: ");
        BigDecimal taxRate = new BigDecimal("1.07");
        if(!scanner.nextLine().isEmpty()){
            taxRate = new BigDecimal(scanner.nextLine());
        }

        Report r = new Report(dataFile, taxRate);

        List<Vehicle> vehicles = r.parseData(r.getDataFile());
        HashMap<Integer, TreeSet<Vehicle>> aggregationMap = r.aggregateVehicles(vehicles);
        r.generateReport(aggregationMap);
    }
}
