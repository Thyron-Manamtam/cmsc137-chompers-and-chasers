package model;

public interface Movable {
    void move(Maze maze);
    int getRow();
    int getCol();
}
