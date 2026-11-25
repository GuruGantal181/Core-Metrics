package coremetrics.models;

/**
 * Abstraction: anything that can describe itself in one human-readable line.
 * Method names deliberately avoid the get/is prefix so Jackson never treats them as JSON properties.
 */
public interface Describable {

    /** One-line description of this object. */
    String describe();

    /** Default method shared by all implementors: "[ClassName] description". */
    default String summaryLine() {
        return "[" + getClass().getSimpleName() + "] " + describe();
    }
}
