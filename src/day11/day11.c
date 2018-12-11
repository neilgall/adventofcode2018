#include <stdio.h>
#include <stdlib.h>

int cells[301][301];

int cell(int x, int y, int serial) {
    int rackId = x + 10;
    int startLevel = rackId * y;
    int value = (startLevel + serial) * rackId;
    return (value / 100) % 10 - 5;
}

void fill(int serial) {
    for (int y = 1; y <= 300; ++y) {
        for (int x = 1; x <= 300; ++x) {
            cells[y][x] = cell(x, y, serial);
        }
    }
}

int square(int x, int y, int size) {
    int total = 0;
    for (int y1 = y, y2 = y + size; y1 < y2; ++y1) {
        for (int x1 = x, x2 = x + size; x1 < x2; ++x1) {
            total += cells[y1][x1];
        }
    }
    return total;
}

void print_square(int x, int y, int size) {
    for (int y1 = y - 1, y2 = y + size + 1; y1 < y2; ++y1) {
        for (int x1 = x - 1, x2 = x + size + 1; x1 < x2; ++x1) {
            printf("%4d", cells[y1][x1]);
        }
        printf("\n");
    }
}

void part1() {
    struct { int value, x, y; } max = { 0, 0, 0 };
    for (int y = 1; y <= 298; ++y) {
        for (int x = 1; x <= 298; ++x) {
            int sq = square(x, y, 3);
            if (sq > max.value) {
                max.value = sq;
                max.x = x;
                max.y = y;
            }
        }
    }
    printf("Part 1: %d,%d (%d)\n", max.x, max.y, max.value);
    print_square(max.x, max.y, 3);
}

void part2() {
    struct { int value, x, y, size; } max = { 0, 0, 0, 0 };
    for (int size = 300; size > 0; --size) {
        int limit = 301-size;
        printf("\r%d...", size); fflush(stdout);
        for (int y = 1; y <= limit; ++y) {
            for (int x = 1; x <= limit; ++x) {
                int sq = square(x, y, size);
                if (sq > max.value) {
                    max.value = sq;
                    max.x = x;
                    max.y = y;
                    max.size = size;
                }
            }
        }
    }
    printf("\rPart 2: %d,%d,%d (%d)\n", max.x, max.y, max.size, max.value);
    print_square(max.x, max.y, max.size);
}

int main(int argc, char *argv[]) {
    int serial = atoi(argv[1]);
    fill(serial);
    part1();
    part2();
}