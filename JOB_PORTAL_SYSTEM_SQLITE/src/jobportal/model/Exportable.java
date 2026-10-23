package jobportal.model;

/**
 * Interface: a contract for objects that can be written as a row of a CSV file.
 * Both Job and Application implement it, so FileHandler can export either of them
 * with the same code (polymorphism).
 */
public interface Exportable {

    /** The first line of the CSV file (column names). */
    String getCsvHeader();

    /** One line of the CSV file (the values of this object). */
    String toCsvRow();
}
