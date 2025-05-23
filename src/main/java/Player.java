import java.util.Map;

public class Player {
    private String id;
    private Map<String, Object> attributes;

    // Default constructor
    public Player() {}

    public Player(String id,  Map<String, Object> attributes) {
        this.id = id;
        this.attributes = attributes;
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String toString() {
        return "Player{id='" + id + "', attributes=" + attributes + '}';
    }
}
