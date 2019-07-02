# Mappings from OpenGL GLSL types to Scala types.

Taken from [OpenGL ES GLSL 3.00 Spec (29 Jan 2016)](https://www.khronos.org/registry/OpenGL/specs/es/3.0/GLSL_ES_Specification_3.00.pdf)


## Basic types

| GLSL  | Scala                            | Meaning |
|-------|----------------------------------|---------|
|void   |scala.Unit                        |for functions that do not return a value|
|bool   |scala.Boolean                     |a conditional type, taking on values of true or false|
|int    |scala.Int                         |a signed integer|
|uint   |TBD                               |an unsigned integer|
|float  |scala.Float                       |a single floating-point scalar|
|vec2   |com.jsuereth.gl.math.Vec2[Float]  |a two-component floating-point vector|
|vec3   |com.jsuereth.gl.math.Vec3[Float]  |a three-component floating-point vector|
|vec4   |com.jsuereth.gl.math.Vec4[Float]  |a four-component floating-point vector|
|bvec2  |com.jsuereth.gl.math.Vec2[Boolean]|a two-component Boolean vector|
|bvec3  |com.jsuereth.gl.math.Vec3[Boolean]|a three-component Boolean vector|
|bvec4  |com.jsuereth.gl.math.Vec4[Boolean]|a four-component Boolean vector|
|ivec2  |com.jsuereth.gl.math.Vec2[Int]    |a two-component signed integer vector|
|ivec3  |com.jsuereth.gl.math.Vec3[Int]    | a three-component signed integer vector|
|ivec4  |com.jsuereth.gl.math.Vec4[Int]    |a four-component signed integer vector|
|uvec2  |com.jsuereth.gl.math.Vec2[Uint]   |a two-component unsigned integer vector|
|uvec3  |com.jsuereth.gl.math.Vec3[Uint]   |a three-component unsigned integer vector|
|uvec4  |com.jsuereth.gl.math.Vec4[UInt]   |a four-component unsigned integer vector|
|mat2   |TBD  |a 2×2 floating-point matrix|
|mat3   |TBD  |a 3×3 floating-point matrix|
|mat4   |com.jsuereth.gl.math.Matrix4[Float]|a 4×4 floating-point matrix|
|mat2x2 |TBD|same as a mat2
|mat2x3 |TBD| a floating-point matrix with 2 columns and 3 rows
|mat2x4 |TBD| a floating-point matrix with 2 columns and 4 rows
|mat3x2 |TBD| a floating-point matrix with 3 columns and 2 rows
|mat3x3 |TBD| same as a mat3
|mat3x4 |TBD| a floating-point matrix with 3 columns and 4 rows
|mat4x2 |TBD| a floating-point matrix with 4 columns and 2 rows
|mat4x3 |TBD| a floating-point matrix with 4 columns and 3 rows
|mat4x4 |com.jsuereth.gl.math.Matrix4x4[Float]| same as a mat4

## Sampler opaque types

| GLSL                 | Scala                    | Meaning |
|----------------------|--------------------------|---------|
|sampler2D             |TBD| a handle for accessing a 2D texture
|sampler3D             |TBD| a handle for accessing a 3D texture
|samplerCube           |TBD| a handle for accessing a cube mapped texture
|samplerCubeShadow     |TBD| a handle for accessing a cube map depth texture with comparison
|sampler2DArray        |TBD| a handle for accessing an 2D array texture
|sampler2DArrayShadow  |TBD| a handle for accessing an 2D array texture
|isampler2D            |TBD| a handle for accessing a signed integer 2D texture
|isampler3D            |TBD| a handle for accessing a signed integer 3D texture
|isamplerCube          |TBD| a handle for accessing a signed integer 2D cube mapped texture
|isampler2DArray       |TBD| a handle for accessing a signed integer 2D array texture
|usampler2D            |TBD| a handle for accessing an unsigned integer 2D texture
|usampler3D            |TBD| a handle for accessing an unsigned integer 3D texture
|usamplerCube          |TBD| a handle for accessing an unsigned integer 2D cube mapped texture
|usampler2DArray       |TBD| a handle for accessing an unsigned integer 2D array texture


## Built in operators

| GLSL  | Scala  |
|-------|--------|
|x + y  | x + y  |
|x - y  | x - y  |
|x * y  | x * y  |
|x / y  | x / y  | 
|x % y  |TBD     |
|x++    |TBD     |
|x--    |TBD     |
|-x     | -x     |
|~x     |TBD     |
|!x     |TBD     |
|x << y |TBD     |
|x >> y |TBD     |
|x <  y |x < y   |
|x <= y |x <= y  |
|x >  y |x > y   |
|x >= y |x >= y  |
|&&|TBD|
|`||`|TBD|

## Built-in trigonometry functions
| GLSL | Scala |
|------|-------|
|radians(degrees)|TBD|
|degress(radians)|TBD|
|sin(angle)|TBD|
|cos(angle)|TBD|
|tan(angle)|TBD|
|asin(x)|TBD|
|acos(x)|TBD|
|atan(x,y)|TBD|
|atan(y_over_x)|TBD|
|sinh(x)|TBD|
|cosh(x)|TBD|
|tanh(x)|TBD|
|asinh(x)|TBD|
|acosh(x)|TBD|
|atanh(x)|TBD|

# Built-in Exponential Functions

| GLSL   | Scala |
|--------|-------|
|pow(x,y)|java.lang.Math.pow(x,y)|
|exp(x)|TBD|
|log(x)|TBD|
|exp2(x)|TBD|
|log2(x)|TBD|
|sqrt(x)|TBD|
|inversesqrt(x)|TBD|

## Built-in Common Functions

| GLSL   | Scala |
|--------|-------|
|abs(x,y)|TBD|
|sign(x)|TBD|
|floor(x)|TBD|
|trunc(x)|TBD|
|round(x)|TBD|
|roundEven(x)|TBD|
|ceil(x)|TBD|
|fract(x)|TBD|
|mod(x,y)|TBD|
|modf(x,y)|TBD|
|min(x,y)|java.lang.Math.min(x,y)|
|max(x,y)|java.lang.Math.max(x,y)|
|clamp(x,minVal, maxVal)|TBD|
|mix(x,y,a)|TBD|
|step(edge,x)|TBD|
|smoothstep(edge1,edge2,x)|TBD|
|isnan(x)|TBD|
|isinf(x)|TBD|
|floatBitsToInt(x)|TBD|
|floatBitsToUint(x)|TBD|
|intBitsToFloat(x)|TBD|
|uintBitsToFloat(x)|TBD|


## TODO - floating point packing ops


## Built-in Geometric Functions

| GLSL          | Scala    |
|---------------|----------|
|length(x)      |x.length  |
|distance(p0,p1)|TBD       |
|dot(x,y)       |x.dot(y)  |
|cross(x,y)     |x.cross(y)|
|normalize(x)   |x.normalize|
|faceforward(N,I,Nref)|TBD|
|reflect(I,N)|TBD|
|refract(I,N,eta)|TBD|


## Built-in Matrix Functions
| GLSL          | Scala    |
|---------------|----------|
|matrixCompMulti(x,y)|TBD|
|outerProduct(x,y)|TBD|
|transpose(x)|TBD|
|determinant(x)|x.determinant|
|inverse(x)|x.inverse|

## TODO translations for section 8.7 and below
