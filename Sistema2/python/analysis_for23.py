import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from scipy.stats import linregress
from concurrent.futures import ProcessPoolExecutor, as_completed
import time

folder_path = './data' 
N_values = range(100, 2001, 200)
num_realizations = 10
m_star = 1.0

def parse_simulation_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    time_data = []
    current_time = -1
    
    for line in lines:
        parts = line.strip().split(';')
        if len(parts) == 5:
            try:
                current_time = float(parts[0])
            except ValueError:
                continue
        elif len(parts) == 10:
            try:
                star_id, x, y, z, vx, vy, vz = [float(p) for p in parts[:7]]
                time_data.append([
                    current_time, int(star_id),
                    x, y, z,
                    vx, vy, vz
                ])
            except (ValueError, IndexError):
                continue
                
    if not time_data:
        return pd.DataFrame()

    df = pd.DataFrame(time_data, columns=['time', 'id', 'x', 'y', 'z', 'vx', 'vy', 'vz'])
    return df

def calculate_half_mass_radius(df_group):
    total_mass = len(df_group) * m_star
    cm_x = (df_group['x'] * m_star).sum() / total_mass
    cm_y = (df_group['y'] * m_star).sum() / total_mass
    cm_z = (df_group['z'] * m_star).sum() / total_mass

    distances = np.sqrt(
        (df_group['x'] - cm_x)**2 +
        (df_group['y'] - cm_y)**2 +
        (df_group['z'] - cm_z)**2
    )
    
    sorted_indices = np.argsort(distances.values)
    
    half_mass_idx_sorted = len(df_group) // 2
    original_index_of_median_star = sorted_indices[half_mass_idx_sorted]
    r_hm = distances.iloc[original_index_of_median_star]
    
    return r_hm

def find_crossing_time(df_rhm, threshold=1.0):
    search_df = df_rhm[df_rhm['time'] > 0.2]
    
    crossed = search_df[search_df['r_hm'] > threshold]
    
    if not crossed.empty:
        return crossed['time'].iloc[0]
    return np.nan

def process_single_simulation(args):
    n, j, filepath = args
    if not os.path.exists(filepath):
        return n, None, np.nan
    
    sim_df = parse_simulation_file(filepath)
    if sim_df.empty:
        return n, None, np.nan
        
    rhm_over_time = sim_df.groupby('time').apply(calculate_half_mass_radius).reset_index(name='r_hm')
    t_star = find_crossing_time(rhm_over_time)
    
    print(f"  > Procesado: sim_{n}_{j}.csv")
    return n, rhm_over_time, t_star

if __name__ == "__main__":
    start_time = time.time()
    
    tasks = []
    for n in N_values:
        for j in range(num_realizations):
            filename = f"sim_{n}_{j}.csv"
            filepath = os.path.join(folder_path, filename)
            tasks.append((n, j, filepath))
            
    realizations_data = {n: {'rhm_dfs': [], 't_stars': []} for n in N_values}

    with ProcessPoolExecutor() as executor:
        futures = [executor.submit(process_single_simulation, task) for task in tasks]
        for future in as_completed(futures):
            n, rhm_df, t_star = future.result()
            if rhm_df is not None:
                realizations_data[n]['rhm_dfs'].append(rhm_df)
            if not np.isnan(t_star):
                realizations_data[n]['t_stars'].append(t_star)

    results = {}
    slopes = {}
    crossing_times = {}

    for n in N_values:
        rhm_dfs = realizations_data[n]['rhm_dfs']
        t_stars = realizations_data[n]['t_stars']
        
        if not rhm_dfs:
            print(f"No se encontraron datos válidos para N = {n}. Saltando.")
            continue
        
        combined_rhm = pd.concat(rhm_dfs)
        mean_rhm = combined_rhm.groupby('time')['r_hm'].mean().reset_index()
        results[n] = mean_rhm
        
        stationary_df = mean_rhm.iloc[len(mean_rhm) // 2:]
        if len(stationary_df) > 1:
            slope, _, _, _, _ = linregress(stationary_df['time'], stationary_df['r_hm'])
            slopes[n] = slope
        else:
            slopes[n] = np.nan
            
        if t_stars:
            crossing_times[n] = np.mean(t_stars)
        else:
            crossing_times[n] = np.nan


    plt.style.use('seaborn-v0_8-whitegrid')

    plt.figure(figsize=(12, 7))
    for n, df in results.items():
        plt.plot(df['time'], df['r_hm'], label=f'N = {n}')
    plt.axhline(1.0, color='red', linestyle='--', label='$r_{hm} = 1$')
    plt.title('Evolución Temporal del Radio de Media Masa Promedio $<r_{hm}(t)>$')
    plt.xlabel('Tiempo (t)')
    plt.ylabel('Radio de Media Masa Promedio $<r_{hm}>$')
    plt.legend()
    plt.grid(True)
    plt.savefig('rhm_evolucion.png')
    plt.show()

    plt.figure(figsize=(10, 6))
    if slopes:
        n_vals = sorted(slopes.keys())
        slope_vals = [slopes[n] for n in n_vals]
        plt.plot(n_vals, slope_vals, 'o-', label='Pendiente estacionaria')
    plt.title('Pendiente del Estado Estacionario vs. Número de Partículas (N)')
    plt.xlabel('Número de Partículas (N)')
    plt.ylabel('Pendiente de $<r_{hm}(t)>$')
    plt.grid(True)
    plt.legend()
    plt.savefig('pendiente_vs_N.png')
    plt.show()

    plt.figure(figsize=(10, 6))
    if crossing_times:
        n_cross_vals = [n for n, t in crossing_times.items() if not np.isnan(t)]
        t_star_vals = [crossing_times[n] for n in n_cross_vals]
        if n_cross_vals:
            plt.plot(n_cross_vals, t_star_vals, 'o-', color='green', label='Tiempo de cruce promedio')
    plt.title('Tiempo de Cruce Promedio $<t^*>$ vs. Número de Partículas (N)')
    plt.xlabel('Número de Partículas (N)')
    plt.ylabel('Tiempo Promedio $<t^*>$ ($r_{hm} > 1$)')
    plt.grid(True)
    plt.legend()
    plt.savefig('t_star_vs_N.png')
    plt.show()