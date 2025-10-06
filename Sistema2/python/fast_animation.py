import pandas as pd
import matplotlib.pyplot as plt
from mpl_toolkits.mplot3d import Axes3D
from matplotlib.animation import FuncAnimation, FFMpegWriter
import numpy as np
from tqdm import tqdm
from pathlib import Path

def parse_simulation_data(filename):
    """
    Analiza el archivo de datos de simulación con el nuevo formato 't=...'.
    Ahora guarda el galaxyId para poder colorear.
    """
    with open(filename, 'r') as f:
        lines = f.readlines()

    particle_data = {}
    all_timesteps = []
    current_time = None
    current_particles = []

    print("Procesando archivo de datos...")
    for line in tqdm(lines, desc="Leyendo archivo"):
        line = line.strip()
        if not line:
            continue

        if line.startswith('t='):
            if current_time is not None and current_particles:
                particle_data[current_time] = np.array(current_particles)

            try:
                current_time = float(line.split('=')[1])
                if current_time not in all_timesteps:
                    all_timesteps.append(current_time)
                current_particles = []
            except (ValueError, IndexError):
                continue
        else:
            parts = line.split(';')
            if len(parts) >= 5:
                try:
                    # --- MEJORA: Guardamos el galaxyId ---
                    galaxy_id = int(parts[1])
                    x = float(parts[2])
                    y = float(parts[3])
                    z = float(parts[4])
                    # Guardamos [galaxy_id, x, y, z]
                    current_particles.append([galaxy_id, x, y, z])
                except (ValueError, IndexError):
                    pass

    if current_time is not None and current_particles:
        particle_data[current_time] = np.array(current_particles)

    all_timesteps.sort()
    return all_timesteps, particle_data


def animate(timesteps, particle_data, output_filename='galaxy_animation.mp4'):
    """
    Crea una animación 3D eficiente, coloreando por galaxia.
    """
    fig = plt.figure(figsize=(10, 8))
    ax = fig.add_subplot(111, projection='3d')

    all_positions = np.vstack([data[:, 1:4] for data in particle_data.values()])
    min_coords = all_positions.min(axis=0)
    max_coords = all_positions.max(axis=0)

    ax.set_xlim([min_coords[0] - 1, max_coords[0] + 1])
    ax.set_ylim([min_coords[1] - 1, max_coords[1] + 1])
    ax.set_zlim([min_coords[2] - 1, max_coords[2] + 1])

    ax.set_xlabel('X')
    ax.set_ylabel('Y')
    ax.set_zlabel('Z')
    title = ax.set_title('')

    # --- MEJORA: Paleta de colores para las galaxias ---
    colors = ['blue', 'red', 'green', 'purple', 'orange']

    # --- MEJORA: Crear el objeto scatter UNA SOLA VEZ ---
    scatter = ax.scatter([], [], [], s=5, alpha=0.7)

    pbar = tqdm(total=len(timesteps), desc="Renderizando video", unit="frame")

    def update(frame):
        # --- MEJORA: NO se usa ax.cla(). Mucho más rápido. ---
        time = timesteps[frame]
        # El array ahora es [galaxy_id, x, y, z]
        positions = particle_data[time]

        # --- MEJORA: Asignar colores según el galaxyId ---
        particle_colors = [colors[int(gid) % len(colors)] for gid in positions[:, 0]]

        # --- MEJORA: Actualizar solo los datos del scatter plot ---
        scatter._offsets3d = (positions[:, 1], positions[:, 2], positions[:, 3])
        scatter.set_color(particle_colors)

        title.set_text(f'Simulación de Galaxias - Tiempo: {time:.3f}')
        pbar.update(1)

        return scatter, title

    ani = FuncAnimation(fig, update, frames=len(timesteps), blit=False)

    print("\nGuardando animación... Esto puede tardar unos minutos.")
    try:
        writer = FFMpegWriter(fps=30, metadata=dict(artist='Me'), bitrate=1800)
        ani.save(output_filename, writer=writer, dpi=150)
    except FileNotFoundError:
        print("\n❌ ERROR: No se encontró el programa 'ffmpeg'.")
    finally:
        pbar.close()

    print(f"\nAnimación guardada como '{output_filename}'")


if __name__ == '__main__':
    filename = Path(__file__).parent / 'data' / 'sim_dt_0.001.csv'

    print(f"Cargando y analizando datos desde '{filename}'...")
    timesteps, data = parse_simulation_data(filename)

    if not data:
        print("No se encontraron datos válidos en el archivo.")
    else:
        print(f"Datos cargados exitosamente. {len(timesteps)} pasos de tiempo encontrados.")
        animate(timesteps, data)