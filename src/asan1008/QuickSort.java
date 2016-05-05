package asan1008;

import java.util.List;

import spacesettlers.objects.Asteroid;

public class QuickSort {
	public static List<Asteroid> quickSort(List<Asteroid> asteroids, int i, int j) {
        if(i<j)
        {
            int pos = partition(asteroids,i,j);
            quickSort(asteroids, i, pos-1);
            quickSort(asteroids, pos+1, j);
        }
        return asteroids;
    }

    private static int partition(List<Asteroid> asteroids,int i,int j) {
        double pivot = asteroids.get(j).getResources().getTotal();
        int small = i-1;
        for(int k = i;k < j;k++)
        {
            if(asteroids.get(k).getResources().getTotal() >= pivot)
            {
                small++;
                swap(asteroids,k,small);
            }
        }
        swap(asteroids,j,small+1);
        return small+1;
    }

    private static void swap(List<Asteroid> asteroids, int k, int small) {
        Asteroid temp;
        temp = asteroids.get(k);
        asteroids.set(k, asteroids.get(small));
        asteroids.set(small, temp);
        
    }
}
