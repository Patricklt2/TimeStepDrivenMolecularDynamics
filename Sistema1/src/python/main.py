import numpy as np
from analysis import basic_oscillator, loader, plots

K = 10000.0
GAMMA = 100.0
MASS = 70.0

def calculate_mse(numerical_pos, analytical_pos):
    return np.mean((numerical_pos - analytical_pos)**2)

def analyze_single_run():
    dt_para_grafico = 1e-4 

    filenames = {
        'Verlet': f'data/raw/verlet_sim_{dt_para_grafico:.0e}.csv'.replace('e-0', 'e-'),
        'Beeman': f'data/raw/beeman_sim_{dt_para_grafico:.0e}.csv'.replace('e-0', 'e-'),
        'Gear':   f'data/raw/gear_sim_{dt_para_grafico:.0e}.csv'.replace('e-0', 'e-')
    }
    
    sim_data_dict = {}
    
    for name, path in filenames.items():
        df = loader.load_simulation_data(path)
        if df is not None:
            sim_data_dict[name] = df

    if not sim_data_dict:
        return

    time_values = sim_data_dict['Verlet']['t'].values
    analytical_pos = basic_oscillator.calculate_analytical_position(time_values, K, MASS, GAMMA)
    
    analytical_data = {'Time': time_values, 'Position': analytical_pos}

    for name, df in sim_data_dict.items():
        mse = calculate_mse(df['pos'].values, analytical_pos)
        print(f"MSE para {name}: {mse:.2e}")

    plots.plot_trajectories(analytical_data, sim_data_dict)

def analyze_error_vs_dt():
    dts = [1e-1, 1e-2, 1e-3, 1e-4, 1e-5, 1e-6, 1e-7]
    algorithms = ['verlet', 'beeman', 'gear']
    error_data = {name: [] for name in algorithms}

    for name in algorithms:
        for dt in dts:
            filename = f'data/raw/{name}_sim_{dt:.0e}.csv'.replace('e-0', 'e-')
            df = loader.load_simulation_data(filename)
            
            if df is not None:
                time_values = df['t'].values
                analytical_pos = basic_oscillator.calculate_analytical_position(time_values, K, MASS, GAMMA)
                mse = calculate_mse(df['pos'].values, analytical_pos)
                error_data[name].append((dt, mse))

    plots.plot_error_vs_dt(error_data)


if __name__ == '__main__':
    analyze_error_vs_dt()