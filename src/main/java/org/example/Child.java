package org.example;

public class Child {
    private int id;
    private String surname;
    private String name;
    private String lastName;
    private int group;

    public Child(int id, String surname, String name, String lastName, int group) {
        this.id = id;
        this.surname = surname;
        this.name = name;
        this.lastName = lastName;
        this.group = group;
    }

    public int getId() { return id; }
    public String getSurname() { return surname; }
    public String getName() { return name; }
    public String getLastName() { return lastName; }
    public int getGroup() { return group; }

    @Override
    public String toString() {
        return id + ": " + surname + " by " + name + " (" + lastName + ", " + group + ")";
    }
}