import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.animation import FuncAnimation
import numpy as np

def parse_simulation_data(filename):
    """
    Analiza el archivo de datos de simulación con un formato personalizado.

    El archivo tiene bloques de datos por cada paso de tiempo. Cada bloque comienza con
    una línea de cabecera de la galaxia y es seguido por líneas de datos de las estrellas.

    Args:
        filename (str): La ruta al archivo CSV.

    Returns:
        tuple: Una tupla conteniendo:
            - all_timesteps (list): Una lista de los valores de tiempo.
            - particle_data (dict): Un diccionario donde las claves son los pasos de tiempo
                                     y los valores son arrays de NumPy con las posiciones
                                     (x, y, z) de las partículas.
    """
    with open(filename, 'r') as f:
        lines = f.readlines()

    particle_data = {}
    all_timesteps = []
    current_time = None
    current_particles = []

    for line in lines:
        parts = line.strip().split(';')
        # La línea de cabecera de la galaxia tiene menos campos que una línea de partícula
        if len(parts) < 9:
            # Si ya estábamos procesando un paso de tiempo, guárdalo
            if current_time is not None and current_particles:
                particle_data[current_time] = np.array(current_particles)
                current_particles = []

            current_time = float(parts[0])
            if current_time not in all_timesteps:
                all_timesteps.append(current_time)
        else:
            # Es una línea de datos de partícula
            # Formato: id;x;y;z;vx;vy;vz;fx;fy;fz
            try:
                # Extraemos las posiciones x, y, z (índices 1, 2, 3)
                x = float(parts[1])
                y = float(parts[2])
                z = float(parts[3])
                current_particles.append([x, y, z])
            except (ValueError, IndexError) as e:
                print(f"Omitiendo línea mal formada: {line.strip()} - Error: {e}")

    # Asegúrate de guardar el último paso de tiempo
    if current_time is not None and current_particles:
        particle_data[current_time] = np.array(current_particles)

    # Ordena los pasos de tiempo por si acaso no están en orden
    all_timesteps.sort()

    # Asegurémonos de que todos los pasos de tiempo tengan la misma cantidad de partículas
    # rellenando si es necesario. Esto puede ocurrir si el archivo termina abruptamente.
    num_particles = 0
    if particle_data:
        num_particles = len(list(particle_data.values())[0])

    for t in all_timesteps:
        if t not in particle_data or particle_data[t].shape[0] < num_particles:
            # Si falta un frame o está incompleto, lo rellenamos con datos del anterior
            # o con ceros si es el primero.
            if all_timesteps.index(t) > 0:
                prev_t = all_timesteps[all_timesteps.index(t) - 1]
                particle_data[t] = particle_data[prev_t]
            else:
                particle_data[t] = np.zeros((num_particles, 3))


    return all_timesteps, particle_data


def animate(timesteps, particle_data, output_filename='galaxy_animation.mp4'):
    """
    Crea y guarda una animación 3D de la simulación de partículas.
    """
    fig = plt.figure(figsize=(10, 8))
    ax = fig.add_subplot(111, projection='3d')

    # Determina los límites de los ejes para que la vista sea estable
    all_positions = np.vstack([data for data in particle_data.values()])
    min_coords = all_positions.min(axis=0)
    max_coords = all_positions.max(axis=0)

    # Dales un poco de margen
    ax.set_xlim([min_coords[0] - 1, max_coords[0] + 1])
    ax.set_ylim([min_coords[1] - 1, max_coords[1] + 1])
    ax.set_zlim([min_coords[2] - 1, max_coords[2] + 1])

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')

    # Objeto de dispersión inicial (vacío)
    scatter = ax.scatter([], [], [], s=5, c='blue', alpha=0.7)
    title = ax.set_title('')

    def update(frame):
        # Limpia los datos anteriores
        ax.cla()

        # Re-establece los límites y etiquetas para cada frame
        ax.set_xlim([min_coords[0] - 1, max_coords[0] + 1])
        ax.set_ylim([min_coords[1] - 1, max_coords[1] + 1])
        ax.set_zlim([min_coords[2] - 1, max_coords[2] + 1])
        ax.set_xlabel('X')
        ax.set_ylabel('Y')
        ax.set_zlabel('Z')

        # Obtiene las posiciones para el frame actual
        time = timesteps[frame]
        positions = particle_data[time]

        # Actualiza el gráfico de dispersión
        scatter = ax.scatter(positions[:, 0], positions[:, 1], positions[:, 2], s=5, c='blue', alpha=0.7)
        title.set_text(f'Simulación de Galaxias - Tiempo: {time:.3f}')

        print(f"Procesando frame {frame+1}/{len(timesteps)}", end='\r')

        return scatter, title

    # Crea la animación
    ani = FuncAnimation(fig, update, frames=len(timesteps), blit=False)

    # Guarda la animación
    print("\nGuardando animación... Esto puede tardar unos minutos.")
    ani.save(output_filename, writer='ffmpeg', fps=30, dpi=150)
    print(f"\nAnimación guardada como '{output_filename}'")


if __name__ == '__main__':
    # Cambia 'sim.csv' por el nombre de tu archivo de datos
    filename = 'sim_dt_0.001.csv'

    print(f"Cargando y analizando datos desde '{filename}'...")
    timesteps, data = parse_simulation_data(filename)

    if not data:
        print("No se encontraron datos válidos en el archivo.")
    else:
        print(f"Datos cargados exitosamente. {len(timesteps)} pasos de tiempo encontrados.")
        animate(timesteps, data)