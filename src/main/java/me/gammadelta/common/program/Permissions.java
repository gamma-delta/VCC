package me.gammadelta.common.program;

/**
 * Permissions to read/write at memory.
 */
public class Permissions {
    public boolean read;
    public boolean write;
    public boolean execute;

    public static Permissions RWX;
    public static Permissions RW;
    public static Permissions R;
    public static Permissions NONE;
    static {
        RWX = new Permissions(true, true, true);
        RW = new Permissions(true, true, false);
        R = new Permissions(true, false, false);
        NONE = new Permissions(false, false, false);
    }

    public Permissions(boolean read, boolean write, boolean execute) {
        this.read = read;
        this.write = write;
        this.execute = execute;
    }

    /** Returns if these Permissions are less restrictive than the other ones. */
    public boolean satisfiedBy(Permissions other) {
        return !((!this.read && other.read) || (!this.write && other.write) || (!this.execute && other.execute));
    }
}
