import java.math.BigDecimal;

public class Vehicle {
    // to distinguish duplicates
    private Integer vehicleId;
    private static Integer vehicleIdCounter = 0;

    private int year;
    private String make;
    private String model;
    private BigDecimal msrp;

    public Vehicle(int year, String make, String model, BigDecimal msrp){
        this.vehicleId = vehicleIdCounter++;
        this.year = year;
        this.make = make;
        this.model = model;
        this.msrp = msrp;
    }

    public Integer getId(){
        return vehicleId;
    }

    public int getYear() {
        return year;
    }

    public String getMake() {
        return make;
    }

    public String getModel() {
        return model;
    }

    public BigDecimal getMsrp() {
        return msrp;
    }
}
