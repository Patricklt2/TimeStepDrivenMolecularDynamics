import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os
from scipy.stats import linregress
from concurrent.futures import ProcessPoolExecutor, as_completed

def parse_simulation_file(filepath):
    with open(filepath, 'r') as f:
        lines = f.readlines()
    
    time_data = []
    current_time = -1
    
    for line in lines:
        line = line.strip()
        if line.startswith('t='):
            try:
                current_time = float(line.split('=')[1].replace(',', '.'))
            except ValueError:
                continue
        else:
            parts = line.split(';')

            if len(parts) == 8:
                try:
                    star_id, galaxyId, x, y, z, vx, vy, vz = [float(p.replace(',', '.')) for p in parts]
                    time_data.append([
                        current_time, int(star_id), int(galaxyId),
                        x, y, z,
                        vx, vy, vz
                    ])
                except (ValueError, IndexError):
                    continue
                
    if not time_data:
        return pd.DataFrame()

    df = pd.DataFrame(time_data, columns=['time', 'id', 'galaxyId', 'x', 'y', 'z', 'vx', 'vy', 'vz'])
    return df

def calculate_half_mass_radius(df_group):
    m_star = 1.0
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
    df = df_rhm.sort_values('time').drop_duplicates(subset='time')
    times = df['time'].values
    rhm = df['r_hm'].values
    
    if len(rhm) < 2:
        return np.nan
        
    for i in range(1, len(rhm)):
        if rhm[i-1] <= threshold < rhm[i]:
            frac = (threshold - rhm[i-1]) / (rhm[i] - rhm[i-1])
            return times[i-1] + frac * (times[i] - times[i-1])
            
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
    
    return n, rhm_over_time, t_star

if __name__ == "__main__":
    N_values = range(100, 2001, 200)
    num_realizations = 10
    m_star = 1.0

    script_dir = os.path.dirname(os.path.abspath(__file__))
    folder_path = os.path.abspath(os.path.join(script_dir, 'data'))

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

    mean_slopes = {}
    slope_stds_from_individuals = {}
    results = {}
    mean_crossing_times = {}
    std_crossing_times = {}

    for n in N_values:
        rhm_dfs = realizations_data[n]['rhm_dfs']
        t_stars = realizations_data[n]['t_stars']
        
        if not rhm_dfs:
            continue
        
        min_time = max(df['time'].min() for df in rhm_dfs)
        max_time = min(df['time'].max() for df in rhm_dfs)
        common_time_grid = np.linspace(min_time, max_time, num=500)
        
        interpolated_rhms = []
        for df in rhm_dfs:
            df_sorted = df.sort_values(by='time').drop_duplicates(subset='time')
            interp_values = np.interp(common_time_grid, df_sorted['time'], df_sorted['r_hm'])
            interpolated_rhms.append(interp_values)
        
        if not interpolated_rhms:
            continue
        
        mean_rhm_values = np.mean(interpolated_rhms, axis=0)
        mean_rhm_df = pd.DataFrame({'time': common_time_grid, 'r_hm': mean_rhm_values})
        results[n] = mean_rhm_df
        
        start_index_stationary = int(len(mean_rhm_df) * 0.6)
        stationary_df_mean = mean_rhm_df.iloc[start_index_stationary:]
        
        if len(stationary_df_mean) > 1:
            slope, _, _, _, _ = linregress(stationary_df_mean['time'], stationary_df_mean['r_hm'])
            mean_slopes[n] = slope
        else:
            mean_slopes[n] = np.nan
        
        individual_slopes = []
        for df_realization in rhm_dfs:
            start_idx = int(len(df_realization) * 0.6)
            stationary_df_individual = df_realization.iloc[start_idx:]
            if len(stationary_df_individual) > 1:
                slope_i, _, _, _, _ = linregress(stationary_df_individual['time'], stationary_df_individual['r_hm'])
                individual_slopes.append(slope_i)
        
        if individual_slopes:
            slope_stds_from_individuals[n] = np.std(individual_slopes)
        else:
            slope_stds_from_individuals[n] = 0

        if t_stars:
            mean_crossing_times[n] = np.mean(t_stars)
            std_crossing_times[n] = np.std(t_stars)
        else:
            mean_crossing_times[n] = np.nan
            std_crossing_times[n] = np.nan
            
    plt.style.use('seaborn-v0_8-whitegrid')

    plt.figure(figsize=(12, 7))
    for n, df in results.items():
        plt.plot(df['time'], df['r_hm'], label=f'N = {n}')
    plt.axhline(1.0, color='red', linestyle='--', label='$r_{hm} = 1$')
    plt.xlabel('Tiempo (t)', fontsize=14)
    plt.ylabel('Radio de Media Masa Promedio $<r_{hm}>$', fontsize=14)
    plt.xticks(fontsize=14)
    plt.yticks(fontsize=14)
    plt.legend()
    plt.grid(True)
    plt.savefig('rhm_evolucion.png')
    plt.show()

    plt.figure(figsize=(10, 6))
    if mean_slopes:
        n_vals = sorted(mean_slopes.keys())
        slope_means = np.array([mean_slopes[n] for n in n_vals])
        slope_stds = [slope_stds_from_individuals.get(n, 0) for n in n_vals]

        if len(n_vals) > 1:
            log_n = np.log(n_vals)
            log_slope = np.log(slope_means)

            k_slope, intercept_slope, _, _, _ = linregress(log_n, log_slope)
            A_slope = np.exp(intercept_slope)
            
            n_fit_slope = np.linspace(min(n_vals), max(n_vals), 100)
            slope_fit = A_slope * (n_fit_slope ** k_slope)
            
            plt.errorbar(n_vals, slope_means, yerr=slope_stds, fmt='o-', color='blue')
            plt.plot(n_fit_slope, slope_fit, 'r--', label=f'Ajuste: m $\propto {A_slope:.2f} N^{{{k_slope:.2f}}}$')

    
    plt.xlabel('Número de Partículas (N)', fontsize=13)
    plt.ylabel('Pendiente de $<r_{hm}(t)>$', fontsize=13)
    plt.xticks(fontsize=13)
    plt.yticks(fontsize=13)
    plt.legend()
    plt.grid(True)
    plt.savefig('pendiente_vs_N.png')
    plt.show()

    plt.figure(figsize=(10, 6))
    if mean_crossing_times:
        n_cross_vals = sorted([n for n, t in mean_crossing_times.items() if not np.isnan(t)])
        t_star_vals = np.array([mean_crossing_times[n] for n in n_cross_vals])
        t_star_stds = [std_crossing_times.get(n, 0) for n in n_cross_vals]
        
        if len(n_cross_vals) > 1:
            log_n = np.log(n_cross_vals)
            log_t_star = np.log(t_star_vals)
            
            k, intercept, r_value, p_value, std_err = linregress(log_n, log_t_star)
            
            A = np.exp(intercept)
            n_fit = np.linspace(min(n_cross_vals), max(n_cross_vals), 100)
            t_star_fit = A * (n_fit ** k)
            
            plt.errorbar(n_cross_vals, t_star_vals, yerr=t_star_stds, fmt='o-', color='green')
            plt.plot(n_fit, t_star_fit, 'r--', label=f'Ajuste: $t^* \propto {A:.2f} N^{{{k:.2f}}}$')

    
    plt.xlabel('Número de Partículas (N)', fontsize=13)
    plt.ylabel('Tiempo Promedio $<t^*>$ ($r_{hm} > 1$)', fontsize=13)
    plt.xticks(fontsize=13)
    plt.yticks(fontsize=13)
    plt.grid(True)
    plt.legend()
    plt.savefig('t_star_vs_N.png')
    plt.show()