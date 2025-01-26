public class Resource {

    private char nameOfResource;
    private int numberOfResource;

    Resource(char name, int number) {
        this.nameOfResource = name;
        this.numberOfResource = number;
    }

    public char getNameOfResource() {
        return nameOfResource;
    }

    public int getNumberOfResource() {
        return numberOfResource;
    }

    public void setNumberOfResource(int numberOfResource) {
        this.numberOfResource = numberOfResource;
    }

    @Override
    public boolean equals(Object o) {
        Resource resc = (Resource) o;
        return nameOfResource == resc.nameOfResource &&
                numberOfResource == resc.numberOfResource;
    }

    @Override
    public String toString() {
        return nameOfResource +
                ":" + numberOfResource;
    }
}
