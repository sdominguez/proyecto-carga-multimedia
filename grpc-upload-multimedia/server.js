const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const fs = require('fs');
const path = require('path');

const PROTO_PATH = './fileuploader.proto';

const packageDefinition = protoLoader.loadSync(PROTO_PATH, {
  keepCase: true,
  longs: String,
  enums: String,
  defaults: true,
  oneofs: true,
});

const uploaderProto = grpc.loadPackageDefinition(packageDefinition).uploader;

const UPLOAD_FOLDER = path.join("C:/workspace/node/static-content-api/public/", 'uploads');

if (!fs.existsSync(UPLOAD_FOLDER)) {
  fs.mkdirSync(UPLOAD_FOLDER, { recursive: true });
}

const uploadFile = (call, callback)=> {
  const { fileName, fileData } = call.request;

   const filePath = path.join(UPLOAD_FOLDER, fileName);

  fs.writeFile(filePath, fileData, (err) => {
    if (err) {
      console.error('Error al guardar el archivo:', err);
      return callback(null, { message: 'Error al guardar el archivo', success: false });
    }

    console.log(`Archivo ${fileName} guardado exitosamente en ${UPLOAD_FOLDER}`);
    callback(null, { message: 'Archivo subido con éxito', success: true });
  });
}

const main = ()=> {
  const server = new grpc.Server();

  server.addService(uploaderProto.FileUploader.service, { UploadFile: uploadFile });

  const hostport = '0.0.0.0:50051';
  server.bindAsync(hostport, grpc.ServerCredentials.createInsecure(), (err, bindPort) => {
    if (err) {
      console.error('Error al iniciar el servidor:', err);
      return;
    }
    console.log(`Servidor gRPC escuchando en ${bindPort}`);
  });
}

main();
