import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
from pathlib import Path
from tqdm import tqdm

def parse_simulation_output(filename):
    """
    Procesa el archivo de salida CSV con el nuevo formato (t=...).
    Retorna un diccionario que mapea el tiempo (float) a un DataFrame de partículas.
    """
    timesteps = {}
    current_time = None
    particle_data = []

    print(f"Leyendo el archivo de simulación: {filename}...")

    with open(filename, 'r') as f:
        lines = f.readlines()

    with tqdm(total=len(lines), desc="Procesando líneas") as pbar:
        for line in lines:
            line = line.strip()
            if not line:
                pbar.update(1)
                continue

            # --- CAMBIO CLAVE: Detectar header de tiempo ---
            if line.startswith('t='):
                if current_time is not None and particle_data:
                    # Para este script, solo necesitamos id, x, y
                    df = pd.DataFrame(particle_data, columns=['id', 'galaxyId', 'x', 'y', 'z'])
                    df = df.apply(pd.to_numeric)
                    timesteps[current_time] = df

                try:
                    current_time = float(line.split('=')[1])
                    particle_data = []
                except (ValueError, IndexError):
                    continue
            # --- CAMBIO CLAVE: Leer línea de partícula ---
            else:
                parts = line.split(';')
                if len(parts) >= 5: # id, galaxyId, x, y, z
                    if current_time is not None:
                        # Solo necesitamos los 5 primeros campos
                        particle_data.append(parts[:5])
            pbar.update(1)

    # Guarda el último bloque de datos
    if current_time is not None and particle_data:
        df = pd.DataFrame(particle_data, columns=['id', 'galaxyId', 'x', 'y', 'z'])
        df = df.apply(pd.to_numeric)
        timesteps[current_time] = df

    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def main():
    """
    Función principal para ejecutar el análisis y graficar las trayectorias.
    """
    # --- CAMBIO CLAVE: Usar Pathlib para una ruta más robusta ---
    # Asegúrate de que el nombre del archivo sea el correcto
    data_file_path = Path(__file__).parent / "data" / "sim_dt_0.001.csv"

    try:
        print(f"Buscando archivo de datos en: {data_file_path.resolve()}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        return

    if not data_by_time:
        print("No se procesaron datos. El archivo podría estar vacío o en un formato incorrecto.")
        return

    # --- Reestructurar datos para graficar trayectorias (sin cambios) ---
    all_data_list = []
    for t, df in data_by_time.items():
        df['time'] = t
        all_data_list.append(df)

    if not all_data_list:
        print("No se encontraron datos de partículas para graficar.")
        return

    full_df = pd.concat(all_data_list, ignore_index=True)

    # --- Configuración del Gráfico (sin cambios) ---
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10), sharex=True)
    fig.suptitle('Evolución de las Posiciones de las Partículas en el Tiempo', fontsize=16)

    for particle_id, group in full_df.groupby('id'):
        sorted_group = group.sort_values('time')
        ax1.plot(sorted_group['time'], sorted_group['x'], alpha=0.8)
        ax2.plot(sorted_group['time'], sorted_group['y'], alpha=0.8)

    ax1.set_ylabel('Coordenada X')
    ax1.set_title('Posición en X vs. Tiempo')
    ax1.grid(True, linestyle='--', alpha=0.6)

    ax2.set_ylabel('Coordenada Y')
    ax2.set_title('Posición en Y vs. Tiempo')
    ax2.set_xlabel('Tiempo de Simulación (s)')
    ax2.grid(True, linestyle='--', alpha=0.6)

    num_particles = len(full_df['id'].unique())
    if num_particles <= 10:
        ax1.legend([f'Partícula {int(pid)}' for pid in full_df['id'].unique()], loc='upper right')
        ax2.legend([f'Partícula {int(pid)}' for pid in full_df['id'].unique()], loc='upper right')
    else:
        print(f"Se omitió la leyenda porque hay demasiadas partículas ({num_particles}).")

    plt.tight_layout(rect=[0, 0, 1, 0.96])
    plt.show()

if __name__ == "__main__":
    main()