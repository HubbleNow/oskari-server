package fi.peltodata.domain;

import fi.nls.oskari.domain.User;
import fi.nls.oskari.domain.map.MaplayerGroup;
import fi.nls.oskari.domain.map.OskariLayer;

import java.util.HashSet;
import java.util.Set;

public class Farmfield implements Comparable<Farmfield> {
    private Long id;
    private String description;
    private MaplayerGroup group;
    private User user;
    private Long userId;
    private Set<OskariLayer> layers = new HashSet<>();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public MaplayerGroup getGroup() {
        return group;
    }

    public void setGroup(MaplayerGroup group) {
        this.group = group;
    }

    public Set<OskariLayer> getLayers() {
        return layers;
    }

    public void addLayer(OskariLayer layer) {
        this.layers.add(layer);
    }

    public void setLayers(Set<OskariLayer> layers) {
        this.layers = layers;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }


    @Override
    public int compareTo(Farmfield o) {
        if (this.getId() == null) {
            return -1;
        } else if (o.getId() == null) {
            return 1;
        }
        else return this.getId().compareTo(o.id);
    }
}
