"""
A Python port of the "Code-It-Yourself! 3D Graphics Engine Part #1 - Triangles & Projection"
From @javidx9.

The link to the video is https://www.youtube.com/watch?v=ih20l3pJoeU

If you are interested in learning 3D graphics, javidx9's videos are very approachable.
This code is not optimized - it is for educational purposes only. There are optimizations
(for example, collapsing the matrix multiplications into one) that are intentionally left
out to enable the student to step through.

I also recommend this video to discover the power of the matrix https://www.youtube.com/watch?v=vQ60rFwh2ig

(c) Jason Chambers 2023
"""
import math
from collections import namedtuple
import pygame as pg
import numpy as np

Vec3d = namedtuple('Vec3d', 'x y z')
Triangle = namedtuple('Triangle', 'p1 p2 p3')

def multiply_matrix_vector(vec3d_in, matrix):
    x = vec3d_in.x * matrix[0][0] + vec3d_in.y * matrix[1][0] + vec3d_in.z * matrix[2][0] + matrix[3][0];
    y = vec3d_in.x * matrix[0][1] + vec3d_in.y * matrix[1][1] + vec3d_in.z * matrix[2][1] + matrix[3][1];
    z = vec3d_in.x * matrix[0][2] + vec3d_in.y * matrix[1][2] + vec3d_in.z * matrix[2][2] + matrix[3][2];
    w = vec3d_in.x * matrix[0][3] + vec3d_in.y * matrix[1][3] + vec3d_in.z * matrix[2][3] + matrix[3][3];
    if w != 0.0:
        x /= w;
        y /= w;
        z /= w;
    return Vec3d(x,y,z)

def gen_proj_matrix(screen_width, screen_height):
    fNear = 0.1
    fFar = 1000.0
    fFov = 90.0
    fAspectRatio = screen_height / screen_width;
    fFovRad = 1.0 / math.tan(fFov * 0.5 / 180.0 * 3.13159);
    return np.array([
        [fAspectRatio * fFovRad, 0,       0,                                0],
        [0,                      fFovRad, 0,                                0],
        [0,                      0,       fFar / (fFar - fNear),            1],
        [0,                      0,       (-fFar * fNear) / (fFar - fNear), 0]
    ])

def translate(triangle_in):
    translated_p1 = Vec3d(triangle_in.p1.x, triangle_in.p1.y, triangle_in.p1.z + 3.0)
    translated_p2 = Vec3d(triangle_in.p2.x, triangle_in.p2.y, triangle_in.p2.z + 3.0)
    translated_p3 = Vec3d(triangle_in.p3.x, triangle_in.p3.y, triangle_in.p3.z + 3.0)
    return Triangle(translated_p1, translated_p2, translated_p3)

def rotate_z(triangle_in, fTheta):
    # Rotate the object on the Z axis
    matrot_z = gen_matrot_z(fTheta)
    rotated_z_p1 = multiply_matrix_vector(triangle_in.p1, matrot_z)
    rotated_z_p2 = multiply_matrix_vector(triangle_in.p2, matrot_z)
    rotated_z_p3 = multiply_matrix_vector(triangle_in.p3, matrot_z)
    return Triangle(rotated_z_p1, rotated_z_p2, rotated_z_p3)

def rotate_x(triangle_in, fTheta):
    # Rotate the object on the X axis
    matrot_x = gen_matrot_x(fTheta)
    rotated_x_p1 = multiply_matrix_vector(triangle_in.p1, matrot_x)
    rotated_x_p2 = multiply_matrix_vector(triangle_in.p2, matrot_x)
    rotated_x_p3 = multiply_matrix_vector(triangle_in.p3, matrot_x)
    return Triangle(rotated_x_p1,rotated_x_p2,rotated_x_p3)

def normal(triangle_in):
    # Calculate the normal of the triangle
    # See the first part of https://www.youtube.com/watch?v=XgMWc6LumG4
    line1_x = triangle_in.p2.x - triangle_in.p1.x 
    line1_y = triangle_in.p2.y - triangle_in.p1.y
    line1_z = triangle_in.p2.z - triangle_in.p1.z

    line2_x = triangle_in.p3.x - triangle_in.p1.x 
    line2_y = triangle_in.p3.y - triangle_in.p1.y
    line2_z = triangle_in.p3.z - triangle_in.p1.z

    normal_x = line1_y * line2_z - line1_z * line2_y
    normal_y = line1_z * line2_x - line1_x * line2_z
    normal_z = line1_x * line2_y - line1_y * line2_x

    l = math.sqrt(normal_x * normal_x + normal_y * normal_y + normal_z * normal_z)
    normal_x /= l
    normal_y /= l
    normal_z /= l
    return Vec3d(normal_x, normal_y, normal_z)

def scale_into_view(vec3d_in, screen_width, screen_height):
    scaled_x = vec3d_in.x
    scaled_y = vec3d_in.y
    scaled_x += 1.0 
    scaled_y += 1.0
    scaled_x *= 0.5 * screen_width
    scaled_y *= 0.5 * screen_height
    return Vec3d(scaled_x, scaled_y, vec3d_in.z)

def gen_matrot_x(a):
    return np.array([
        [1, 0,            0,           0],
        [0, math.cos(a),  math.sin(a), 0],
        [0, -math.sin(a), math.cos(a), 0],
        [0, 0,            0,           1]
    ])

def gen_matrot_z(a):
    return np.array([
        [math.cos(a),  math.sin(a), 0, 0],
        [-math.sin(a), math.cos(a), 0, 0],
        [0,            0,           1, 0],
        [0,            0,           0, 1]
    ])

class Cube:
    def __init__(self) -> None:
        pg.init()
        self.RES = self.WIDTH, self.HEIGHT = 1000,1000
        self.FPS = 10
        self.screen = pg.display.set_mode(self.RES)
        self.clock = pg.time.Clock()
        self.fTheta = 0
        self.vCamera = Vec3d(0,0,0)
        self.mesh_cube = [
            # SOUTH
            Triangle(Vec3d(0,0,0), Vec3d(0,1,0), Vec3d(1,1,0)),
            Triangle(Vec3d(0,0,0), Vec3d(1,1,0), Vec3d(1,0,0)),
    
            # EAST
            Triangle(Vec3d(1,0,0), Vec3d(1,1,0), Vec3d(1,1,1)),
            Triangle(Vec3d(1,0,0), Vec3d(1,1,1), Vec3d(1,0,1)),
    
            # NORTH
            Triangle(Vec3d(1,0,1), Vec3d(1,1,1), Vec3d(0,1,1)),
            Triangle(Vec3d(1,0,1), Vec3d(0,1,1), Vec3d(0,0,1)),
    
            # WEST
            Triangle(Vec3d(0,0,1), Vec3d(0,1,1), Vec3d(0,1,0)),
            Triangle(Vec3d(0,0,1), Vec3d(0,1,0), Vec3d(0,0,0)),
    
            # TOP
            Triangle(Vec3d(0,1,0), Vec3d(0,1,1), Vec3d(1,1,1)),
            Triangle(Vec3d(0,1,0), Vec3d(1,1,1), Vec3d(1,1,0)),
    
            # BOTTOM
            Triangle(Vec3d(1,0,1), Vec3d(0,0,1), Vec3d(0,0,0)),
            Triangle(Vec3d(1,0,1), Vec3d(0,0,0), Vec3d(1,0,0))
        ]

    def draw(self):
        # Clear the previous frame
        self.screen.fill((255,255,255))

        self.fTheta += 0.1

        for tri in self.mesh_cube:
            #Rotate the triangle first on the Z axis, then on the X axis
            tri = rotate_z(tri, self.fTheta)
            tri = rotate_x(tri, self.fTheta)

            # Ensure the triangle is in front of the viewer
            tri = translate(tri)

            # Calculate the normal of the triangle - so we can decide if it's visible or not
            normal_vec = normal(tri)

            #if normal_vec.z < 0:
            if normal_vec.x * (tri.p1.x - self.vCamera.x) + normal_vec.y * (tri.p1.y - self.vCamera.y) + normal_vec.z * (tri.p1.z - self.vCamera.z) < 0:
                # Project from 3D to 2D
                mat_proj = gen_proj_matrix(self.WIDTH, self.HEIGHT)
                projected_p1 = multiply_matrix_vector(tri.p1,mat_proj)
                projected_p2 = multiply_matrix_vector(tri.p2,mat_proj)
                projected_p3 = multiply_matrix_vector(tri.p3,mat_proj)

                # Scale it up
                scaled_p1 = scale_into_view(projected_p1, self.WIDTH, self.HEIGHT)
                scaled_p2 = scale_into_view(projected_p2, self.WIDTH, self.HEIGHT)
                scaled_p3 = scale_into_view(projected_p3, self.WIDTH, self.HEIGHT)
                tri_projected_and_scaled = Triangle(scaled_p1, scaled_p2, scaled_p3)

                # Now draw it
                self.draw_triangle(tri_projected_and_scaled)

    def draw_triangle(self,tri):
        x1 = tri.p1.x 
        y1 = tri.p1.y
        x2 = tri.p2.x 
        y2 = tri.p2.y
        x3 = tri.p3.x 
        y3 = tri.p3.y

        pg.draw.line(self.screen, (255,0,0), (x1, y1), (x2, y2))
        pg.draw.line(self.screen, (255,0,0), (x2, y2), (x3, y3))
        pg.draw.line(self.screen, (255,0,0), (x3, y3), (x1, y1))
    
    def run(self):
        while True:
            self.draw()
            [exit() for e in pg.event.get() if e.type == pg.QUIT]
            pg.display.set_caption(str(self.clock.get_fps()))
            pg.display.flip() # What does this do?
            self.clock.tick(self.FPS)

if __name__ == '__main__':
    app = Cube()
    app.run()