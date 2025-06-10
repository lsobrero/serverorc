package dev.sbr.location.model;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Random;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import io.quarkus.hibernate.orm.panache.PanacheEntity;

@Entity
public class Location extends PanacheEntity {

    @NotNull
    @Size(min = 3, max = 50)
    public String name;

    public static List<Location> listAllWhereNameLike(String name) {
        return (name != null) ?
                list("LOWER(name) LIKE CONCAT('%', ?1, '%')", name.toLowerCase()) :
                List.of();
    }

    public static Optional<Location> findRandom() {
        var countLocations = count();

        if (countLocations > 0) {
            var randomLocation = new Random().nextInt((int) countLocations);
            return findAll().page(randomLocation, 1).firstResultOptional();
        }

        return Optional.empty();
    }


    @Override
    /* prettier-ignore */
    public String toString() {
        return (
                "Location{" +
                        "id=" + this.id +
                        ", name='" + this.name +
                        '}'
        );
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Location location = (Location) o;
        return this.id.equals(location.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.id);
    }
}
