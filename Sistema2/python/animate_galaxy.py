"""
Animación 3D de Simulación de Galaxias
Optimizado para rendimiento con grandes conjuntos de datos

Estructura de archivos:
    python/
    ├── animate_galaxy.py  (este archivo)
    └── data/
        └── test.csv
"""

import numpy as np
import matplotlib.pyplot as plt
from matplotlib.animation import FuncAnimation, PillowWriter, FFMpegWriter
from mpl_toolkits.mplot3d import Axes3D
import pandas as pd
from pathlib import Path
import time
from tqdm import tqdm
import logging as log

log.basicConfig(level=log.INFO, format='%(asctime)s - %(levelname)s - %(message)s')

class GalaxyAnimator:
    def __init__(self, csv_file='data/sim.csv'):
        """
        Inicializa el animador de galaxias
        
        Parameters:
        -----------
        csv_file : str
            Ruta relativa al archivo CSV desde la carpeta python/
        """
        self.csv_path = Path(__file__).parent / csv_file
        self.time_steps = []
        self.num_particles = 0
        self.colors = ['#00d4ff', '#ff6b6b', '#00ff88', '#ffaa00']  # Colores para diferentes partículas
        self.pbar = None  # Barra de progreso
        
    def load_data(self):
        """Carga y procesa los datos del CSV de forma eficiente"""
        print(f"📂 Cargando datos desde: {self.csv_path}")
        start_time = time.time()
        
        if not self.csv_path.exists():
            raise FileNotFoundError(f"No se encontró el archivo: {self.csv_path}")
        
        # Leer archivo línea por línea para mayor eficiencia
        with open(self.csv_path, 'r') as f:
            lines = f.readlines()
        
        current_time = None
        current_particles = []
        
        # Barra de progreso para carga de datos
        with tqdm(total=len(lines), desc="Procesando líneas", unit="línea") as pbar:
            for line in lines:
                parts = line.strip().split(';')
                
                # Línea de encabezado de tiempo (contiene "Galaxy_")
                if len(parts) >= 2 and 'Galaxy' in str(parts[1]):
                    # Guardar el paso de tiempo anterior
                    if current_time is not None and current_particles:
                        self.time_steps.append({
                            'time': current_time,
                            'data': np.array(current_particles)
                        })
                    
                    # Iniciar nuevo paso de tiempo
                    current_time = float(parts[0])
                    current_particles = []
                    
                # Línea de datos de partícula
                elif len(parts) >= 4:
                    try:
                        # Parsear: ID, x, y, z, vx, vy, vz, ...
                        particle_id = int(parts[0])
                        x = float(parts[1])
                        y = float(parts[2])
                        z = float(parts[3])
                        current_particles.append([particle_id, x, y, z])
                    except (ValueError, IndexError):
                        pass
                
                pbar.update(1)
        
        # Agregar el último paso de tiempo
        if current_time is not None and current_particles:
            self.time_steps.append({
                'time': current_time,
                'data': np.array(current_particles)
            })
        
        if self.time_steps:
            self.num_particles = len(self.time_steps[0]['data'])
        
        elapsed = time.time() - start_time
        print(f"✅ Datos cargados en {elapsed:.2f}s")
        print(f"   - Pasos de tiempo: {len(self.time_steps)}")
        print(f"   - Partículas por paso: {self.num_particles}")
        print(f"   - Tiempo inicial: {self.time_steps[0]['time']:.6f}")
        print(f"   - Tiempo final: {self.time_steps[-1]['time']:.6f}")
        
    def setup_plot(self):
        """Configura la figura y los ejes 3D"""
        print("🎨 Configurando visualización 3D...")
        
        self.fig = plt.figure(figsize=(12, 9))
        self.ax = self.fig.add_subplot(111, projection='3d')
        
        # Calcular límites de los ejes basados en todos los datos
        all_data = np.vstack([step['data'][:, 1:4] for step in self.time_steps])
        margins = 0.2
        
        x_min, x_max = all_data[:, 0].min(), all_data[:, 0].max()
        y_min, y_max = all_data[:, 1].min(), all_data[:, 1].max()
        z_min, z_max = all_data[:, 2].min(), all_data[:, 2].max()
        
        x_range = x_max - x_min
        y_range = y_max - y_min
        z_range = z_max - z_min
        
        self.ax.set_xlim(x_min - margins * x_range, x_max + margins * x_range)
        self.ax.set_ylim(y_min - margins * y_range, y_max + margins * y_range)
        self.ax.set_zlim(z_min - margins * z_range, z_max + margins * z_range)
        
        # Etiquetas
        self.ax.set_xlabel('X', fontsize=12, fontweight='bold')
        self.ax.set_ylabel('Y', fontsize=12, fontweight='bold')
        self.ax.set_zlabel('Z', fontsize=12, fontweight='bold')
        
        # Título
        self.title = self.ax.text2D(0.05, 0.95, '', transform=self.ax.transAxes,
                                     fontsize=14, verticalalignment='top',
                                     bbox=dict(boxstyle='round', facecolor='black', alpha=0.7))
        
        # Configuración visual
        self.ax.grid(True, alpha=0.3)
        self.ax.set_facecolor('#000511')
        self.fig.patch.set_facecolor('#000511')
        
        # Inicializar scatter plots para cada partícula
        self.scatters = []
        for i in range(self.num_particles):
            color = self.colors[i % len(self.colors)]
            scatter = self.ax.scatter([], [], [], c=color, s=100, alpha=0.8, 
                                     edgecolors='white', linewidths=0.5, 
                                     label=f'Partícula {i}')
            self.scatters.append(scatter)
        
        # Agregar trazas (líneas que muestran la trayectoria)
        self.traces = []
        self.trace_length = min(10, len(self.time_steps))  # Mostrar últimos 10 pasos
        
        for i in range(self.num_particles):
            color = self.colors[i % len(self.colors)]
            line, = self.ax.plot([], [], [], c=color, alpha=0.3, linewidth=1)
            self.traces.append(line)
        
        self.ax.legend(loc='upper right', fontsize=10)
        
    def init_animation(self):
        """Inicializa la animación"""
        for scatter in self.scatters:
            scatter._offsets3d = ([], [], [])
        for trace in self.traces:
            trace.set_data([], [])
            trace.set_3d_properties([])
        return self.scatters + self.traces + [self.title]
    
    def update_frame(self, frame):
        """Actualiza cada frame de la animación"""
        step = self.time_steps[frame]
        data = step['data']
        
        # Actualizar posiciones de partículas
        for i in range(self.num_particles):
            particle_data = data[data[:, 0] == i]
            if len(particle_data) > 0:
                x, y, z = particle_data[0, 1:4]
                self.scatters[i]._offsets3d = ([x], [y], [z])
        
        # Actualizar trazas (trayectorias)
        start_frame = max(0, frame - self.trace_length)
        for i in range(self.num_particles):
            trace_x, trace_y, trace_z = [], [], []
            
            for f in range(start_frame, frame + 1):
                particle_data = self.time_steps[f]['data']
                particle_data = particle_data[particle_data[:, 0] == i]
                if len(particle_data) > 0:
                    trace_x.append(particle_data[0, 1])
                    trace_y.append(particle_data[0, 2])
                    trace_z.append(particle_data[0, 3])
            
            if trace_x:
                self.traces[i].set_data(trace_x, trace_y)
                self.traces[i].set_3d_properties(trace_z)
        
        # Actualizar título con información
        self.title.set_text(f'Simulación de Galaxias\n'
                           f'Paso: {frame + 1}/{len(self.time_steps)}\n'
                           f'Tiempo: {step["time"]:.6f}')
        
        # Rotar la vista automáticamente para mejor visualización
        self.ax.view_init(elev=20, azim=frame * 0.5)
        
        # Actualizar barra de progreso
        if self.pbar is not None:
            self.pbar.update(1)
        
        return self.scatters + self.traces + [self.title]
    
    def create_animation(self, output_file='galaxy_animation.gif', 
                        fps=30, dpi=100, interval=50):
        """
        Crea y guarda la animación
        
        Parameters:
        -----------
        output_file : str
            Nombre del archivo de salida (soporta .gif, .mp4)
        fps : int
            Frames por segundo
        dpi : int
            Resolución de la imagen
        interval : int
            Intervalo entre frames en milisegundos
        """
        print(f"\n🎬 Creando animación...")
        print(f"   - Frames totales: {len(self.time_steps)}")
        print(f"   - FPS: {fps}")
        print(f"   - Intervalo: {interval}ms")
        
        start_time = time.time()
        
        # Crear barra de progreso
        print("   Generando frames...")
        self.pbar = tqdm(total=len(self.time_steps), desc="Renderizando", unit="frame")
        
        # Crear animación
        anim = FuncAnimation(
            self.fig, 
            self.update_frame,
            init_func=self.init_animation,
            frames=len(self.time_steps),
            interval=interval,
            blit=False,  # Mejor para 3D
            repeat=True
        )
        
        # Guardar según el formato
        output_path = Path(__file__).parent / output_file
        
        if output_file.endswith('.gif'):
            print(f"💾 Guardando como GIF: {output_path}")
            writer = PillowWriter(fps=fps)
            anim.save(str(output_path), writer=writer, dpi=dpi)
        elif output_file.endswith('.mp4'):
            print(f"💾 Guardando como MP4: {output_path}")
            writer = FFMpegWriter(fps=fps, bitrate=1800)
            anim.save(str(output_path), writer=writer, dpi=dpi)
        else:
            raise ValueError("Formato no soportado. Use .gif o .mp4")
        
        # Cerrar barra de progreso
        if self.pbar is not None:
            self.pbar.close()
            self.pbar = None
        
        elapsed = time.time() - start_time
        print(f"✅ Animación guardada en {elapsed:.2f}s")
        print(f"   Archivo: {output_path}")
        
        return anim
    
    def show_interactive(self):
        """Muestra la animación de forma interactiva"""
        print("\n🎬 Mostrando animación interactiva...")
        print("   (Cierra la ventana para continuar)")
        
        anim = FuncAnimation(
            self.fig, 
            self.update_frame,
            init_func=self.init_animation,
            frames=len(self.time_steps),
            interval=50,
            blit=False,
            repeat=True
        )
        
        plt.show()


def main():
    """Función principal"""
    print("="*60)
    print("🌌 ANIMADOR DE SIMULACIÓN DE GALAXIAS 3D")
    print("="*60)
    
    # Crear instancia del animador
    animator = GalaxyAnimator(csv_file='data/sim.csv')
    
    # Cargar datos
    animator.load_data()
    
    # Configurar plot
    animator.setup_plot()
    
    # Opciones de salida
    print("\n📋 Opciones:")
    print("   1. Guardar como GIF (recomendado)")
    print("   2. Guardar como MP4 (requiere FFmpeg)")
    print("   3. Mostrar animación interactiva")
    
    choice = input("\nSeleccione una opción (1-3) [1]: ").strip() or "1"
    
    if choice == "1":
        animator.create_animation(
            output_file='galaxy_animation.gif',
            fps=20,
            dpi=100,
            interval=50
        )
    elif choice == "2":
        animator.create_animation(
            output_file='galaxy_animation.mp4',
            fps=30,
            dpi=150,
            interval=33
        )
    elif choice == "3":
        animator.show_interactive()
    else:
        print("❌ Opción no válida")
        return
    
    print("\n✨ Proceso completado!")


if __name__ == "__main__":
    main()