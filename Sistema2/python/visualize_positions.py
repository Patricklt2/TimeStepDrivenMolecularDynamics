import pandas as pd
import numpy as np
import matplotlib.pyplot as plt
import os

def parse_simulation_output(filename="sim.csv"):
    """
    Procesa el archivo de salida CSV de la simulación.
    Esta versión está corregida para manejar múltiples galaxias por paso de tiempo,
    concatenando los datos si un paso de tiempo aparece varias veces.
    """
    timesteps = {}
    current_time = None
    particle_data = []

    print(f"Leyendo el archivo de simulación: {filename}...")

    with open(filename, 'r') as f:
        for line in f:
            line = line.strip()
            if not line:
                continue

            parts = line.split(';')

            # Una línea de encabezado de galaxia tiene 5 partes
            if len(parts) == 5 and 'Galaxy' in parts[1]:
                # Si ya teníamos datos de partículas, los guardamos antes de empezar un nuevo bloque.
                if current_time is not None and particle_data:
                    new_df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
                    new_df = new_df.apply(pd.to_numeric)

                    # Si el tiempo ya existe, concatena los DataFrames (para múltiples galaxias)
                    if current_time in timesteps:
                        timesteps[current_time] = pd.concat([timesteps[current_time], new_df], ignore_index=True)
                    else:
                        timesteps[current_time] = new_df

                # Empezamos un nuevo bloque
                try:
                    current_time = float(parts[0])
                    particle_data = [] # Reinicia la lista para las partículas de esta nueva galaxia
                except (ValueError, IndexError):
                    current_time = None # Ignora encabezados mal formateados
                    continue
            # Una línea de partícula tiene 10 partes
            elif len(parts) == 10:
                if current_time is not None:
                    particle_data.append(parts)

    # Guarda el último bloque de datos que quedó en memoria
    if current_time is not None and particle_data:
        new_df = pd.DataFrame(particle_data, columns=['id', 'x', 'y', 'z', 'vx', 'vy', 'vz', 'fx', 'fy', 'fz'])
        new_df = new_df.apply(pd.to_numeric)
        if current_time in timesteps:
            timesteps[current_time] = pd.concat([timesteps[current_time], new_df], ignore_index=True)
        else:
            timesteps[current_time] = new_df

    print(f"Se procesaron {len(timesteps)} pasos de tiempo.")
    return timesteps

def main():
    """
    Función principal para ejecutar el análisis y graficar las trayectorias.
    """
    # Construcción de la ruta al archivo de datos
    try:
        script_dir = os.path.dirname(os.path.abspath(__file__))
        data_file_path = os.path.join(script_dir, "data", "sim_dt_0.001.csv")
    except NameError:
        # Fallback para cuando se ejecuta en un entorno interactivo
        data_file_path = "python/data/sim.csv"

    try:
        print(f"Buscando archivo de datos en: {os.path.abspath(data_file_path)}")
        data_by_time = parse_simulation_output(data_file_path)
    except FileNotFoundError:
        print(f"Error: No se encontró el archivo en '{data_file_path}'.")
        print("Asegúrate de que la simulación se haya ejecutado y guardado el archivo correctamente.")
        return

    if not data_by_time:
        print("No se procesaron datos. El archivo podría estar vacío o en un formato incorrecto.")
        return

    # --- Reestructurar datos para graficar trayectorias ---
    # Convertir el diccionario de DataFrames en un único DataFrame grande
    all_data_list = []
    for t, df in data_by_time.items():
        df['time'] = t
        all_data_list.append(df)

    if not all_data_list:
        print("No se encontraron datos de partículas para graficar.")
        return

    full_df = pd.concat(all_data_list, ignore_index=True)

    # --- Configuración del Gráfico ---
    # Crear una figura con dos subplots apilados verticalmente que comparten el eje X
    fig, (ax1, ax2) = plt.subplots(2, 1, figsize=(14, 10), sharex=True)

    fig.suptitle('Evolución de las Posiciones de las Partículas en el Tiempo', fontsize=16)

    # Agrupar por ID de partícula y graficar la trayectoria de cada una
    for particle_id, group in full_df.groupby('id'):
        # Ordenar por tiempo para que las líneas se dibujen correctamente
        sorted_group = group.sort_values('time')

        # Graficar Coordenada X vs. Tiempo
        ax1.plot(sorted_group['time'], sorted_group['x'], label=f'Partícula {int(particle_id)}', alpha=0.8)

        # Graficar Coordenada Y vs. Tiempo
        ax2.plot(sorted_group['time'], sorted_group['y'], label=f'Partícula {int(particle_id)}', alpha=0.8)

    # --- Estilo y Etiquetas ---
    ax1.set_ylabel('Coordenada X')
    ax1.set_title('Posición en X vs. Tiempo')
    ax1.grid(True, linestyle='--', alpha=0.6)

    ax2.set_ylabel('Coordenada Y')
    ax2.set_title('Posición en Y vs. Tiempo')
    ax2.set_xlabel('Tiempo de Simulación (s)')
    ax2.grid(True, linestyle='--', alpha=0.6)

    # Mostrar una leyenda solo si hay pocas partículas para no saturar el gráfico
    num_particles = len(full_df['id'].unique())
    if num_particles <= 10:
        ax1.legend(loc='upper right')
        ax2.legend(loc='upper right')
    else:
        print(f"Se omitió la leyenda porque hay demasiadas partículas ({num_particles}).")

    plt.tight_layout(rect=[0, 0, 1, 0.96]) # Ajustar para que el título principal no se superponga
    plt.show()

if __name__ == "__main__":
    main()